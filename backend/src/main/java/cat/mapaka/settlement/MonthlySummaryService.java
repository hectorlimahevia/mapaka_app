package cat.mapaka.settlement;

import cat.mapaka.allowance.AllowanceStatus;
import cat.mapaka.allowance.MonthlyAllowanceRepository;
import cat.mapaka.child.ChildProfile;
import cat.mapaka.child.ChildProfileRepository;
import cat.mapaka.common.TransactionType;
import cat.mapaka.family.FamilyRepository;
import cat.mapaka.money.MoneySourceType;
import cat.mapaka.money.MoneyTransaction;
import cat.mapaka.money.MoneyTransactionRepository;
import cat.mapaka.money.WalletType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Resum mensual: el que ha ENTRAT al fill cada mes, calculat sempre a partir del ledger —
 * no és una foto congelada. El mes en curs arriba fins ara; un mes passat queda tal com va
 * acabar l'últim dia, perquè els moviments del ledger mai es reescriuen.
 *
 * Només compten els ingressos (paga, tasques, bonificacions) i les penalitzacions. Els
 * traspassos interns (estalvi, aportacions a objectius), les compres i les donacions no
 * són diners nous del mes i queden fora.
 */
@Service
public class MonthlySummaryService {

    private static final List<MoneySourceType> SOURCES = List.of(
            MoneySourceType.MONTHLY_ALLOWANCE, MoneySourceType.TASK, MoneySourceType.BONUS,
            MoneySourceType.MANUAL_ADJUSTMENT, MoneySourceType.PENALTY, MoneySourceType.TASK_PENALTY,
            MoneySourceType.GOAL_CONTRIBUTION);

    /** Les files d'un mateix ingrés (gastar, estalvi i objectius) es desen a la mateixa
     * transacció, amb la mateixa descripció i a pocs mil·lisegons una de l'altra. */
    private static final Duration SAME_EVENT_WINDOW = Duration.ofSeconds(2);

    private enum Line { ALLOWANCE, TASKS, BONUS }

    private static final class Month {
        final Map<Line, BigDecimal> income = new EnumMap<>(Line.class);
        BigDecimal penalties = BigDecimal.ZERO;
        BigDecimal spending = BigDecimal.ZERO;
        BigDecimal savings = BigDecimal.ZERO;
        BigDecimal goals = BigDecimal.ZERO;

        BigDecimal line(Line line) {
            return income.getOrDefault(line, BigDecimal.ZERO);
        }

        void addIncome(Line line, BigDecimal amount) {
            income.merge(line, amount, BigDecimal::add);
        }
    }

    private final FamilyRepository familyRepository;
    private final ChildProfileRepository childProfileRepository;
    private final MoneyTransactionRepository moneyTransactionRepository;
    private final MonthlyAllowanceRepository monthlyAllowanceRepository;

    public MonthlySummaryService(
            FamilyRepository familyRepository,
            ChildProfileRepository childProfileRepository,
            MoneyTransactionRepository moneyTransactionRepository,
            MonthlyAllowanceRepository monthlyAllowanceRepository) {
        this.familyRepository = familyRepository;
        this.childProfileRepository = childProfileRepository;
        this.moneyTransactionRepository = moneyTransactionRepository;
        this.monthlyAllowanceRepository = monthlyAllowanceRepository;
    }

    @Transactional(readOnly = true)
    public List<MonthlySummaryResponse> forFamily(UUID familyId) {
        ZoneId zone = ZoneId.of(familyRepository.findById(familyId).orElseThrow().getTimezone());
        YearMonth now = YearMonth.now(zone);

        Map<UUID, List<MoneyTransaction>> rowsByChild = new HashMap<>();
        for (MoneyTransaction t : moneyTransactionRepository.findByFamilyIdAndSourceTypes(familyId, SOURCES)) {
            rowsByChild.computeIfAbsent(t.getChild().getId(), id -> new ArrayList<>()).add(t);
        }

        List<ChildProfile> children = childProfileRepository.findAllActiveByFamilyIdFetchUser(familyId).stream()
                .sorted(Comparator.comparing(ChildProfile::getBirthDate)
                        .thenComparing(ChildProfile::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<MonthlySummaryResponse> result = new ArrayList<>();
        for (ChildProfile child : children) {
            Map<YearMonth, Month> months = summarise(rowsByChild.getOrDefault(child.getId(), List.of()), zone);
            months.computeIfAbsent(now, ym -> new Month());
            months.forEach((ym, month) -> result.add(toResponse(child, ym, month, ym.equals(now))));
        }
        // Mesos recents primer; dins d'un mes, el fill gran primer (l'ordre de `children`
        // es manté perquè el sort és estable).
        result.sort(Comparator.comparing((MonthlySummaryResponse r) -> YearMonth.of(r.year(), r.month())).reversed());
        return result;
    }

    private Map<YearMonth, Month> summarise(List<MoneyTransaction> rows, ZoneId zone) {
        List<MoneyTransaction> anchors = rows.stream().filter(MonthlySummaryService::isSplitIncomeRow).toList();
        Map<YearMonth, Month> months = new HashMap<>();

        for (MoneyTransaction t : rows) {
            Month month = months.computeIfAbsent(YearMonth.from(t.getCreatedAt().atZone(zone)), ym -> new Month());
            if (isGoalIncomeRow(t)) {
                month.addIncome(lineOfGoalRow(t, anchors), t.getAmount());
                month.goals = month.goals.add(t.getAmount());
            } else if (isSplitIncomeRow(t)) {
                month.addIncome(lineOf(t.getSourceType()), t.getAmount());
                if (t.getWalletType() == WalletType.SPENDING) {
                    month.spending = month.spending.add(t.getAmount());
                } else {
                    month.savings = month.savings.add(t.getAmount());
                }
            } else if (isPenaltyRow(t)) {
                month.penalties = month.penalties.add(t.getAmount());
                if (t.getWalletType() == WalletType.SAVINGS) {
                    month.savings = month.savings.subtract(t.getAmount());
                } else {
                    month.spending = month.spending.subtract(t.getAmount());
                }
            }
        }
        // Un mes només amb moviments ignorats (p. ex. un traspàs) no ha de sortir a la llista.
        months.values().removeIf(m -> m.income.isEmpty() && m.penalties.signum() == 0);
        return months;
    }

    /** Ingrés que va a gastar o a estalvi (les files GOAL s'atribueixen a part). */
    private static boolean isSplitIncomeRow(MoneyTransaction t) {
        MoneySourceType s = t.getSourceType();
        return t.getTransactionType() == TransactionType.CREDIT
                && (t.getWalletType() == WalletType.SPENDING || t.getWalletType() == WalletType.SAVINGS)
                && (s == MoneySourceType.MONTHLY_ALLOWANCE || s == MoneySourceType.TASK
                    || s == MoneySourceType.BONUS || s == MoneySourceType.MANUAL_ADJUSTMENT);
    }

    /** Part d'un ingrés que `MoneySplitCalculator` ha destinat a un objectiu. Les aportacions
     * voluntàries i els retorns porten `transferReferenceId`: són traspassos, no ingressos. */
    private static boolean isGoalIncomeRow(MoneyTransaction t) {
        return t.getSourceType() == MoneySourceType.GOAL_CONTRIBUTION
                && t.getWalletType() == WalletType.GOAL
                && t.getTransactionType() == TransactionType.CREDIT
                && t.getTransferReferenceId() == null;
    }

    private static boolean isPenaltyRow(MoneyTransaction t) {
        MoneySourceType s = t.getSourceType();
        return t.getTransactionType() == TransactionType.DEBIT
                && (s == MoneySourceType.PENALTY || s == MoneySourceType.TASK_PENALTY
                    || s == MoneySourceType.MANUAL_ADJUSTMENT);
    }

    private static Line lineOf(MoneySourceType source) {
        return switch (source) {
            case MONTHLY_ALLOWANCE -> Line.ALLOWANCE;
            case TASK -> Line.TASKS;
            default -> Line.BONUS;
        };
    }

    /** La fila GOAL no sap d'on ve (el seu source_id és l'objectiu): es compta amb l'ingrés
     * germà més proper — mateixa descripció i a l'instant — i, si no n'hi ha cap (tot
     * l'ingrés ha anat a objectius), com a bonificació. */
    private static Line lineOfGoalRow(MoneyTransaction goalRow, List<MoneyTransaction> anchors) {
        return anchors.stream()
                .filter(a -> Objects.equals(a.getDescription(), goalRow.getDescription()))
                .filter(a -> Duration.between(a.getCreatedAt(), goalRow.getCreatedAt()).abs().compareTo(SAME_EVENT_WINDOW) <= 0)
                .min(Comparator.comparing(a -> Duration.between(a.getCreatedAt(), goalRow.getCreatedAt()).abs()))
                .map(a -> lineOf(a.getSourceType()))
                .orElse(Line.BONUS);
    }

    private MonthlySummaryResponse toResponse(ChildProfile child, YearMonth ym, Month m, boolean current) {
        BigDecimal allowance = m.line(Line.ALLOWANCE);
        BigDecimal tasks = m.line(Line.TASKS);
        BigDecimal bonuses = m.line(Line.BONUS);
        BigDecimal total = allowance.add(tasks).add(bonuses).subtract(m.penalties);
        boolean pending = current && monthlyAllowanceRepository
                .findByChildIdAndYearAndMonth(child.getId(), ym.getYear(), ym.getMonthValue())
                .map(a -> a.getStatus() == AllowanceStatus.DRAFT).orElse(false);
        return new MonthlySummaryResponse(
                child.getId(), child.getDisplayName(), child.getColorTheme(), child.getAvatarIcon(),
                ym.getYear(), ym.getMonthValue(), current, pending,
                total, allowance, tasks, bonuses, m.penalties, m.spending, m.savings, m.goals);
    }
}
