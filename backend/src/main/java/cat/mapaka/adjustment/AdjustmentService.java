package cat.mapaka.adjustment;

import cat.mapaka.allowance.MoneySplitCalculator;
import cat.mapaka.child.ChildProfile;
import cat.mapaka.common.DomainException;
import cat.mapaka.common.TransactionType;
import cat.mapaka.money.MoneySourceType;
import cat.mapaka.money.MoneyTransaction;
import cat.mapaka.money.MoneyTransactionRepository;
import cat.mapaka.money.WalletType;
import cat.mapaka.screentime.ScreenSourceType;
import cat.mapaka.screentime.ScreenTimeTransaction;
import cat.mapaka.screentime.ScreenTimeTransactionRepository;
import cat.mapaka.user.User;
import cat.mapaka.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Bonificació/penalització puntual sense passar per una tasca (Prompt 9 ampliat) — el
 * PARENT tria un import positiu i el tipus (BONUS/PENALTY) decideix el sentit del moviment
 * al ledger corresponent. */
@Service
public class AdjustmentService {

    private final AdjustmentRepository adjustmentRepository;
    private final ScreenTimeTransactionRepository screenTimeTransactionRepository;
    private final MoneyTransactionRepository moneyTransactionRepository;
    private final UserRepository userRepository;
    private final MoneySplitCalculator moneySplitCalculator;

    public AdjustmentService(
            AdjustmentRepository adjustmentRepository,
            ScreenTimeTransactionRepository screenTimeTransactionRepository,
            MoneyTransactionRepository moneyTransactionRepository,
            UserRepository userRepository,
            MoneySplitCalculator moneySplitCalculator) {
        this.adjustmentRepository = adjustmentRepository;
        this.screenTimeTransactionRepository = screenTimeTransactionRepository;
        this.moneyTransactionRepository = moneyTransactionRepository;
        this.userRepository = userRepository;
        this.moneySplitCalculator = moneySplitCalculator;
    }

    /** Per defecte (RULE) el PARENT només introdueix un Valor i el repartiment es calcula
     * amb el percentatge vigent del fill, igual que a l'aprovació d'una tasca; en una
     * bonificació pot triar enviar-la a gastar, a estalvi o dividir-la ell mateix (un regal
     * d'un familiar no ha de seguir el repartiment de la paga). Una penalització sempre
     * descompta amb el repartiment habitual. */
    @Transactional
    public void applyMoney(ChildProfile child, MoneyAdjustmentRequest request, UUID actingUserId) {
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("INVALID_ADJUSTMENT", HttpStatus.BAD_REQUEST, "Cal un import superior a 0");
        }
        AdjustmentDestination destination = request.destination() == null ? AdjustmentDestination.RULE : request.destination();
        if (request.type() == AdjustmentType.PENALTY && destination != AdjustmentDestination.RULE) {
            throw invalidDestination("El destí només s'aplica a les bonificacions");
        }

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal spendingPart = BigDecimal.ZERO;
        if (destination != AdjustmentDestination.RULE && amount.signum() <= 0) {
            throw new DomainException("INVALID_ADJUSTMENT", HttpStatus.BAD_REQUEST, "Cal un import superior a 0");
        }
        if (destination == AdjustmentDestination.CUSTOM) {
            if (request.spendingAmount() == null) {
                throw invalidDestination("Cal indicar quant va a gastar");
            }
            spendingPart = request.spendingAmount().setScale(2, RoundingMode.HALF_UP);
            if (spendingPart.signum() < 0 || spendingPart.compareTo(amount) > 0) {
                throw invalidDestination("L'import per gastar ha d'estar entre 0 i el total");
            }
        }

        User parent = userRepository.getReferenceById(actingUserId);
        TransactionType txType = request.type() == AdjustmentType.PENALTY ? TransactionType.DEBIT : TransactionType.CREDIT;
        MoneySourceType sourceType = switch (request.type()) {
            case BONUS -> MoneySourceType.BONUS;
            case PENALTY -> MoneySourceType.PENALTY;
        };

        // El desglossament real (gastar/estalvi/objectius) viu als MoneyTransaction — aquí
        // només queda l'import total com a auditoria.
        Adjustment adjustment = adjustmentRepository.save(Adjustment.builder()
                .child(child).adjustmentType(request.type())
                .moneyAmount(request.amount()).savingsAmount(BigDecimal.ZERO).screenMinutes(0)
                .reason(request.reason()).createdBy(parent).build());

        switch (destination) {
            case RULE -> moneySplitCalculator.apply(
                    child, request.amount(), txType, sourceType, adjustment.getId(), request.reason(), parent);
            case SPENDING -> credit(child, WalletType.SPENDING, amount, sourceType, adjustment.getId(), request.reason(), parent);
            case SAVINGS -> credit(child, WalletType.SAVINGS, amount, sourceType, adjustment.getId(), request.reason(), parent);
            case CUSTOM -> {
                credit(child, WalletType.SPENDING, spendingPart, sourceType, adjustment.getId(), request.reason(), parent);
                credit(child, WalletType.SAVINGS, amount.subtract(spendingPart), sourceType, adjustment.getId(), request.reason(), parent);
            }
        }
    }

    /** Com es repartiria `amount` amb el repartiment habitual del fill (gastar, estalvi i
     * objectius) — el mateix càlcul que `applyMoney` amb RULE, sense desar res. */
    @Transactional(readOnly = true)
    public MoneySplitPreviewResponse previewRule(ChildProfile child, BigDecimal amount) {
        MoneySplitCalculator.SplitPlan plan = moneySplitCalculator.plan(child, amount);
        List<MoneySplitPreviewResponse.Part> parts = new ArrayList<>();
        if (plan.spendingAmount().signum() > 0) {
            parts.add(new MoneySplitPreviewResponse.Part(WalletType.SPENDING, null, plan.spendingAmount()));
        }
        for (MoneySplitCalculator.GoalPart goalPart : plan.goalParts()) {
            if (goalPart.amount().signum() > 0) {
                parts.add(new MoneySplitPreviewResponse.Part(WalletType.GOAL, goalPart.goal().getName(), goalPart.amount()));
            }
        }
        if (plan.savingsAmount().signum() > 0) {
            parts.add(new MoneySplitPreviewResponse.Part(WalletType.SAVINGS, null, plan.savingsAmount()));
        }
        return new MoneySplitPreviewResponse(parts);
    }

    private void credit(
            ChildProfile child, WalletType wallet, BigDecimal amount, MoneySourceType sourceType,
            UUID sourceId, String reason, User actor) {
        if (amount.signum() <= 0) {
            return;
        }
        moneyTransactionRepository.save(MoneyTransaction.builder()
                .child(child).walletType(wallet).transactionType(TransactionType.CREDIT)
                .amount(amount).description(reason)
                .sourceType(sourceType).sourceId(sourceId).createdBy(actor).build());
    }

    private DomainException invalidDestination(String message) {
        return new DomainException("INVALID_ADJUSTMENT_DESTINATION", HttpStatus.BAD_REQUEST, message);
    }

    @Transactional
    public void applyScreenTime(ChildProfile child, ScreenTimeAdjustmentRequest request, UUID actingUserId) {
        if (request.minutes() <= 0) {
            throw new DomainException("INVALID_ADJUSTMENT", HttpStatus.BAD_REQUEST, "Cal un nombre de minuts superior a 0");
        }
        User parent = userRepository.getReferenceById(actingUserId);
        TransactionType txType = request.type() == AdjustmentType.PENALTY ? TransactionType.DEBIT : TransactionType.CREDIT;
        ScreenSourceType sourceType = switch (request.type()) {
            case BONUS -> ScreenSourceType.BONUS;
            case PENALTY -> ScreenSourceType.PENALTY;
        };

        Adjustment adjustment = adjustmentRepository.save(Adjustment.builder()
                .child(child).adjustmentType(request.type())
                .moneyAmount(BigDecimal.ZERO).savingsAmount(BigDecimal.ZERO).screenMinutes(request.minutes())
                .reason(request.reason()).createdBy(parent).build());

        ZoneId familyZone = ZoneId.of(child.getUser().getFamily().getTimezone());
        screenTimeTransactionRepository.save(ScreenTimeTransaction.builder()
                .child(child).transactionType(txType).minutes(request.minutes())
                .description(request.reason()).sourceType(sourceType).sourceId(adjustment.getId())
                .occurredOn(LocalDate.now(familyZone)).createdBy(parent).build());
    }
}
