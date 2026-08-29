package cat.mapaka.allowance;

import cat.mapaka.child.ChildProfile;
import cat.mapaka.child.ChildProfileRepository;
import cat.mapaka.common.DomainException;
import cat.mapaka.common.TransactionType;
import cat.mapaka.money.MoneySourceType;
import cat.mapaka.money.MoneyTransactionRepository;
import cat.mapaka.money.WalletType;
import cat.mapaka.screentime.ScreenSourceType;
import cat.mapaka.screentime.ScreenTimeRule;
import cat.mapaka.screentime.ScreenTimeRuleRepository;
import cat.mapaka.screentime.ScreenTimeTransaction;
import cat.mapaka.screentime.ScreenTimeTransactionRepository;
import cat.mapaka.settlement.MonthlySettlement;
import cat.mapaka.settlement.MonthlySettlementRepository;
import cat.mapaka.settlement.SettlementStatus;
import cat.mapaka.user.User;
import cat.mapaka.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Generar la paga del mes (Prompt 9 ampliat, secció 8 de mapaka_documento_global.md) —
 * flux de dos passos perquè un import equivocat no es pugui aplicar per error: generate()
 * només crea files DRAFT, confirm() és qui realment mou diner al ledger. Des del Prompt 16
 * (punt 14/24 de la verificació), confirm() també acredita el temps de pantalla mensual
 * del fill en el mateix acte — abans es generava per separat i diàriament
 * (ScreenTimeService/DailyBaseCreditor), cosa que no coincidia amb el que ja
 * mostrava/editava Fills en termes mensuals (×4 setmanal).
 */
@Service
public class AllowanceGenerationService {

    private final ChildProfileRepository childProfileRepository;
    private final AllowanceRuleRepository allowanceRuleRepository;
    private final MonthlyAllowanceRepository monthlyAllowanceRepository;
    private final MoneyTransactionRepository moneyTransactionRepository;
    private final MonthlySettlementRepository monthlySettlementRepository;
    private final UserRepository userRepository;
    private final MoneySplitCalculator moneySplitCalculator;
    private final ScreenTimeRuleRepository screenTimeRuleRepository;
    private final ScreenTimeTransactionRepository screenTimeTransactionRepository;

    public AllowanceGenerationService(
            ChildProfileRepository childProfileRepository,
            AllowanceRuleRepository allowanceRuleRepository,
            MonthlyAllowanceRepository monthlyAllowanceRepository,
            MoneyTransactionRepository moneyTransactionRepository,
            MonthlySettlementRepository monthlySettlementRepository,
            UserRepository userRepository,
            MoneySplitCalculator moneySplitCalculator,
            ScreenTimeRuleRepository screenTimeRuleRepository,
            ScreenTimeTransactionRepository screenTimeTransactionRepository) {
        this.childProfileRepository = childProfileRepository;
        this.allowanceRuleRepository = allowanceRuleRepository;
        this.monthlyAllowanceRepository = monthlyAllowanceRepository;
        this.moneyTransactionRepository = moneyTransactionRepository;
        this.monthlySettlementRepository = monthlySettlementRepository;
        this.userRepository = userRepository;
        this.moneySplitCalculator = moneySplitCalculator;
        this.screenTimeRuleRepository = screenTimeRuleRepository;
        this.screenTimeTransactionRepository = screenTimeTransactionRepository;
    }

    @Transactional
    public List<MonthlyAllowanceResponse> generate(UUID familyId) {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();
        List<AllowanceRule> generalRules = allowanceRuleRepository
                .findByFamilyIdAndChildIsNullAndActiveTrueOrderByMinAgeAsc(familyId);

        return childProfileRepository.findAllByFamilyId(familyId).stream()
                .filter(ChildProfile::isActive)
                .filter(ChildProfile::isAllowanceEnabled)
                .filter(child -> monthlyAllowanceRepository.findByChildIdAndYearAndMonth(child.getId(), year, month).isEmpty())
                .map(child -> generateFor(child, year, month, generalRules))
                .flatMap(Optional::stream)
                .map(MonthlyAllowanceResponse::from)
                .toList();
    }

    private Optional<MonthlyAllowance> generateFor(ChildProfile child, int year, int month, List<AllowanceRule> generalRules) {
        AllowanceRule rule = allowanceRuleRepository.findByChildIdAndActiveTrue(child.getId())
                .or(() -> resolveGeneralRule(generalRules, child.getAge()))
                .orElse(null);
        if (rule == null) {
            return Optional.empty();
        }

        BigDecimal spendingAmount = rule.getMonthlyAmount()
                .multiply(rule.getSpendingPercentage())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal savingsAmount = rule.getMonthlyAmount().subtract(spendingAmount);

        return Optional.of(monthlyAllowanceRepository.save(MonthlyAllowance.builder()
                .child(child)
                .year(year)
                .month(month)
                .grossAmount(rule.getMonthlyAmount())
                .spendingAmount(spendingAmount)
                .savingsAmount(savingsAmount)
                .allowanceRule(rule)
                .status(AllowanceStatus.DRAFT)
                .build()));
    }

    private Optional<AllowanceRule> resolveGeneralRule(List<AllowanceRule> generalRules, int age) {
        return generalRules.stream()
                .filter(r -> r.getMinAge() == null || age >= r.getMinAge())
                .filter(r -> r.getMaxAge() == null || age <= r.getMaxAge())
                .findFirst();
    }

    @Transactional
    public MonthlyAllowanceResponse confirm(MonthlyAllowance allowance, UUID actingUserId) {
        requireDraft(allowance);
        User parent = userRepository.getReferenceById(actingUserId);
        ChildProfile child = allowance.getChild();

        // El repartiment autoritatiu es calcula aquí, no es reutilitza l'estimació del
        // DRAFT (generate()): el fill pot haver activat un objectiu entremig, i el
        // repartiment real ha de reflectir els objectius actius en confirmar, no abans.
        MoneySplitCalculator.SplitResult split = moneySplitCalculator.apply(
                child, allowance.getGrossAmount(), TransactionType.CREDIT,
                MoneySourceType.MONTHLY_ALLOWANCE, allowance.getId(), "Paga mensual", parent);
        creditMonthlyScreenTime(child, allowance, parent);

        allowance.setStatus(AllowanceStatus.CONFIRMED);
        allowance.setConfirmedAt(Instant.now());
        allowance.setConfirmedBy(parent);
        monthlyAllowanceRepository.save(allowance);

        closeSettlement(allowance, split, parent);
        return MonthlyAllowanceResponse.from(allowance);
    }

    /** Mateix acte que la paga en diners (checklist Prompt 16, punt 14): el fill té com a
     * molt una regla activa sense dia de la setmana (ScreenTimeRule.weekday sempre null,
     * ChildManagementService.updateScreenTimeRule) amb els minuts MENSUALS ja calculats
     * (Fills els mostra/edita en setmanal i els multiplica per 4 abans de desar-los). Es
     * crea com a molt un moviment per fill i mes gràcies al mateix gate de `requireDraft`
     * que ja evita confirmar dos cops la mateixa paga. */
    private void creditMonthlyScreenTime(ChildProfile child, MonthlyAllowance allowance, User parent) {
        Optional<ScreenTimeRule> rule = screenTimeRuleRepository.findByChildIdAndWeekdayIsNullAndActiveTrue(child.getId());
        if (rule.isEmpty() || rule.get().getBaseMinutes() <= 0) {
            return;
        }
        ZoneId zone = ZoneId.of(child.getUser().getFamily().getTimezone());
        screenTimeTransactionRepository.save(ScreenTimeTransaction.builder()
                .child(child).transactionType(TransactionType.CREDIT)
                .minutes(rule.get().getBaseMinutes()).description("Temps de pantalla mensual")
                .sourceType(ScreenSourceType.MONTHLY_BASE).sourceId(allowance.getId())
                .occurredOn(LocalDate.now(zone)).createdBy(parent).build());
    }

    @Transactional
    public MonthlyAllowanceResponse cancel(MonthlyAllowance allowance) {
        requireDraft(allowance);
        allowance.setStatus(AllowanceStatus.CANCELLED);
        monthlyAllowanceRepository.save(allowance);
        return MonthlyAllowanceResponse.from(allowance);
    }

    /** El tancament mensual és el resum de la paga confirmada més el que s'ha guanyat/perdut
     * amb tasques i ajustos aquell mes — es tanca en confirmar la paga, no abans, perquè
     * "extraEarnings"/"bonuses"/"penalties" reflecteixin un mes ja acabat de facto. Fa
     * servir el repartiment real (`split`), no l'estimació guardada al DRAFT — pot diferir
     * si el fill té algun objectiu actiu que es reparteix del "per gastar". */
    private void closeSettlement(MonthlyAllowance allowance, MoneySplitCalculator.SplitResult split, User parent) {
        ChildProfile child = allowance.getChild();
        ZoneId zone = ZoneId.of(child.getUser().getFamily().getTimezone());
        YearMonth ym = YearMonth.of(allowance.getYear(), allowance.getMonth());
        Instant from = ym.atDay(1).atStartOfDay(zone).toInstant();
        Instant to = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();

        BigDecimal extraEarnings = moneyTransactionRepository.sumBySource(
                child.getId(), WalletType.SPENDING, MoneySourceType.TASK, TransactionType.CREDIT, from, to);
        BigDecimal bonuses = moneyTransactionRepository.sumBySource(
                child.getId(), WalletType.SPENDING, MoneySourceType.BONUS, TransactionType.CREDIT, from, to);
        BigDecimal penalties = moneyTransactionRepository.sumBySource(
                child.getId(), WalletType.SPENDING, MoneySourceType.PENALTY, TransactionType.DEBIT, from, to);
        BigDecimal payable = split.spendingAmount().add(extraEarnings).add(bonuses).subtract(penalties);

        MonthlySettlement settlement = monthlySettlementRepository
                .findByChildIdAndYearAndMonth(child.getId(), allowance.getYear(), allowance.getMonth())
                .orElseGet(() -> MonthlySettlement.builder().child(child).year(allowance.getYear()).month(allowance.getMonth()).build());

        settlement.setBaseAllowance(split.spendingAmount());
        settlement.setExtraEarnings(extraEarnings);
        settlement.setBonuses(bonuses);
        settlement.setPenalties(penalties);
        settlement.setSavings(split.savingsAmount());
        settlement.setPayableAmount(payable);
        settlement.setStatus(SettlementStatus.CLOSED);
        settlement.setClosedAt(Instant.now());
        settlement.setClosedBy(parent);
        monthlySettlementRepository.save(settlement);
    }

    private void requireDraft(MonthlyAllowance allowance) {
        if (allowance.getStatus() != AllowanceStatus.DRAFT) {
            throw new DomainException("ALLOWANCE_ALREADY_RESOLVED", HttpStatus.CONFLICT,
                    "Aquesta paga ja s'ha " + (allowance.getStatus() == AllowanceStatus.CONFIRMED ? "confirmat" : "cancel·lat"));
        }
    }
}
