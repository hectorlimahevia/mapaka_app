package cat.mapaka.allowance;

import cat.mapaka.child.ChildProfile;
import cat.mapaka.common.TransactionType;
import cat.mapaka.money.MoneySourceType;
import cat.mapaka.money.MoneyTransaction;
import cat.mapaka.money.MoneyTransactionRepository;
import cat.mapaka.money.WalletType;
import cat.mapaka.savings.SavingsGoal;
import cat.mapaka.savings.SavingsGoalRepository;
import cat.mapaka.savings.SavingsGoalStatus;
import cat.mapaka.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Reparteix un import en N parts (gastar / estalvi / cada objectiu actiu) i crea els
 * moviments corresponents — punt únic cridat des de la generació de la paga mensual,
 * les Bonificacions i l'aprovació de recompensa de tasca (Prompt 15, perquè cap dels
 * tres repliqui aquesta lògica). Substitueix l'antic repartiment binari de MoneySplit. */
@Service
public class MoneySplitCalculator {

    private final AllowanceRuleService allowanceRuleService;
    private final SavingsGoalRepository savingsGoalRepository;
    private final MoneyTransactionRepository moneyTransactionRepository;

    public MoneySplitCalculator(
            AllowanceRuleService allowanceRuleService,
            SavingsGoalRepository savingsGoalRepository,
            MoneyTransactionRepository moneyTransactionRepository) {
        this.allowanceRuleService = allowanceRuleService;
        this.savingsGoalRepository = savingsGoalRepository;
        this.moneyTransactionRepository = moneyTransactionRepository;
    }

    /** Import realment acreditat (o debitat) a gastar i a estalvi, després de restar el
     * que s'ha destinat als objectius actius — útil per a qui necessiti saber el resultat
     * exacte del repartiment (per exemple, el tancament mensual). */
    public record SplitResult(BigDecimal spendingAmount, BigDecimal savingsAmount) {
    }

    /** Reparteix `totalAmount` per a `child` i crea els MoneyTransaction resultants
     * (SPENDING, SAVINGS i un per cada objectiu ACTIVE amb allocation_percentage > 0).
     * `sourceId` identifica l'origen (tasca, ajust, paga) per a les files SPENDING/SAVINGS;
     * les files GOAL sempre queden vinculades al savings_goal_id, no a `sourceId`. */
    @Transactional
    public SplitResult apply(
            ChildProfile child, BigDecimal totalAmount, TransactionType transactionType,
            MoneySourceType sourceType, UUID sourceId, String description, User actor) {
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new SplitResult(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal spendingPercentage = allowanceRuleService.resolveSpendingPercentage(child);
        BigDecimal savingsPercentage = new BigDecimal("100").subtract(spendingPercentage);
        List<SavingsGoal> activeGoals = savingsGoalRepository.findByChildIdAndStatus(child.getId(), SavingsGoalStatus.ACTIVE);

        BigDecimal goalPercentageTotal = activeGoals.stream()
                .map(SavingsGoal::getAllocationPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal effectiveSpendingPercentage = spendingPercentage.subtract(goalPercentageTotal).max(BigDecimal.ZERO);

        BigDecimal spendingAmount = percentageOf(totalAmount, effectiveSpendingPercentage);
        BigDecimal remainingAfterSpending = totalAmount.subtract(spendingAmount);

        BigDecimal goalAmountsSum = BigDecimal.ZERO;
        for (SavingsGoal goal : activeGoals) {
            if (goal.getAllocationPercentage().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal goalAmount = percentageOf(totalAmount, goal.getAllocationPercentage());
            goalAmountsSum = goalAmountsSum.add(goalAmount);
            if (goalAmount.compareTo(BigDecimal.ZERO) > 0) {
                moneyTransactionRepository.save(MoneyTransaction.builder()
                        .child(child).walletType(WalletType.GOAL).transactionType(transactionType)
                        .amount(goalAmount).description(description)
                        .sourceType(MoneySourceType.GOAL_CONTRIBUTION).sourceId(goal.getId())
                        .createdBy(actor).build());
                checkCompletion(goal);
            }
        }

        // L'estalvi absorbeix el residu d'arrodoniment, igual que ja feia el repartiment binari.
        BigDecimal savingsAmount = remainingAfterSpending.subtract(goalAmountsSum);

        if (spendingAmount.compareTo(BigDecimal.ZERO) > 0) {
            moneyTransactionRepository.save(MoneyTransaction.builder()
                    .child(child).walletType(WalletType.SPENDING).transactionType(transactionType)
                    .amount(spendingAmount).description(description)
                    .sourceType(sourceType).sourceId(sourceId).createdBy(actor).build());
        }
        if (savingsAmount.compareTo(BigDecimal.ZERO) > 0) {
            moneyTransactionRepository.save(MoneyTransaction.builder()
                    .child(child).walletType(WalletType.SAVINGS).transactionType(transactionType)
                    .amount(savingsAmount).description(description)
                    .sourceType(sourceType).sourceId(sourceId).createdBy(actor).build());
        }

        return new SplitResult(spendingAmount, savingsAmount);
    }

    private void checkCompletion(SavingsGoal goal) {
        if (goal.getStatus() != SavingsGoalStatus.ACTIVE) {
            return;
        }
        BigDecimal progress = moneyTransactionRepository.goalProgress(goal.getId());
        if (progress.compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(SavingsGoalStatus.COMPLETED);
            goal.setCompletedAt(Instant.now());
            savingsGoalRepository.save(goal);
        }
    }

    private BigDecimal percentageOf(BigDecimal total, BigDecimal percentage) {
        return total.multiply(percentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
