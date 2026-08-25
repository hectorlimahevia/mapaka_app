package cat.mapaka.adjustment;

import cat.mapaka.allowance.MoneySplitCalculator;
import cat.mapaka.child.ChildProfile;
import cat.mapaka.common.DomainException;
import cat.mapaka.common.TransactionType;
import cat.mapaka.money.MoneySourceType;
import cat.mapaka.screentime.ScreenSourceType;
import cat.mapaka.screentime.ScreenTimeTransaction;
import cat.mapaka.screentime.ScreenTimeTransactionRepository;
import cat.mapaka.user.User;
import cat.mapaka.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/** Bonificació/penalització puntual sense passar per una tasca (Prompt 9 ampliat) — el
 * PARENT tria un import positiu i el tipus (BONUS/PENALTY/MANUAL) decideix el sentit del
 * moviment al ledger corresponent. */
@Service
public class AdjustmentService {

    private final AdjustmentRepository adjustmentRepository;
    private final ScreenTimeTransactionRepository screenTimeTransactionRepository;
    private final UserRepository userRepository;
    private final MoneySplitCalculator moneySplitCalculator;

    public AdjustmentService(
            AdjustmentRepository adjustmentRepository,
            ScreenTimeTransactionRepository screenTimeTransactionRepository,
            UserRepository userRepository,
            MoneySplitCalculator moneySplitCalculator) {
        this.adjustmentRepository = adjustmentRepository;
        this.screenTimeTransactionRepository = screenTimeTransactionRepository;
        this.userRepository = userRepository;
        this.moneySplitCalculator = moneySplitCalculator;
    }

    /** L'usuari introdueix un únic Valor — mai el reparteix ell mateix entre gastar i
     * estalviar (Prompt 9 corregit): el reparto es calcula aquí amb el percentatge vigent
     * del fill, igual que a l'aprovació d'una tasca (ApprovalService). */
    @Transactional
    public void applyMoney(ChildProfile child, MoneyAdjustmentRequest request, UUID actingUserId) {
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("INVALID_ADJUSTMENT", HttpStatus.BAD_REQUEST, "Cal un import superior a 0");
        }
        User parent = userRepository.getReferenceById(actingUserId);
        TransactionType txType = request.type() == AdjustmentType.PENALTY ? TransactionType.DEBIT : TransactionType.CREDIT;
        MoneySourceType sourceType = switch (request.type()) {
            case BONUS -> MoneySourceType.BONUS;
            case PENALTY -> MoneySourceType.PENALTY;
            case MANUAL -> MoneySourceType.MANUAL_ADJUSTMENT;
        };

        // El desglossament real (gastar/estalvi/objectius) viu als MoneyTransaction que
        // crea el MoneySplitCalculator — aquí només queda l'import total com a auditoria.
        Adjustment adjustment = adjustmentRepository.save(Adjustment.builder()
                .child(child).adjustmentType(request.type())
                .moneyAmount(request.amount()).savingsAmount(BigDecimal.ZERO).screenMinutes(0)
                .reason(request.reason()).createdBy(parent).build());

        moneySplitCalculator.apply(child, request.amount(), txType, sourceType, adjustment.getId(), request.reason(), parent);
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
            case MANUAL -> ScreenSourceType.MANUAL_ADJUSTMENT;
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
