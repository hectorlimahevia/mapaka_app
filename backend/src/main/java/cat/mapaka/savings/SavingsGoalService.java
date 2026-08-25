package cat.mapaka.savings;

import cat.mapaka.allowance.AllowanceRuleService;
import cat.mapaka.child.ChildProfile;
import cat.mapaka.common.DomainException;
import cat.mapaka.money.MoneyTransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Objectius d'estalvi amb percentatge propi (Prompt 15) — la suma dels `allocationPercentage`
 * de tots els objectius ACTIVE d'un fill mai pot superar el seu percentatge de gastar vigent,
 * perquè MoneySplitCalculator no acabi restant més del que hi ha. */
@Service
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final MoneyTransactionRepository moneyTransactionRepository;
    private final AllowanceRuleService allowanceRuleService;

    public SavingsGoalService(
            SavingsGoalRepository savingsGoalRepository,
            MoneyTransactionRepository moneyTransactionRepository,
            AllowanceRuleService allowanceRuleService) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.moneyTransactionRepository = moneyTransactionRepository;
        this.allowanceRuleService = allowanceRuleService;
    }

    @Transactional(readOnly = true)
    public List<SavingsGoalResponse> list(UUID childId) {
        return savingsGoalRepository.findByChildId(childId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SavingsGoalResponse create(ChildProfile child, CreateSavingsGoalRequest request) {
        BigDecimal allocation = allocationOrZero(request);
        requireAllocationFits(child, allocation, null);
        SavingsGoal goal = savingsGoalRepository.save(SavingsGoal.builder()
                .child(child)
                .name(request.name())
                .targetAmount(request.targetAmount())
                .allocationPercentage(allocation)
                .status(SavingsGoalStatus.ACTIVE)
                .build());
        return toResponse(goal);
    }

    @Transactional
    public SavingsGoalResponse update(SavingsGoal goal, CreateSavingsGoalRequest request) {
        BigDecimal allocation = allocationOrZero(request);
        requireAllocationFits(goal.getChild(), allocation, goal.getId());
        goal.setName(request.name());
        goal.setTargetAmount(request.targetAmount());
        goal.setAllocationPercentage(allocation);
        savingsGoalRepository.save(goal);
        return toResponse(goal);
    }

    public SavingsGoal requireOwnedBy(UUID goalId, UUID childId) {
        SavingsGoal goal = savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new DomainException("SAVINGS_GOAL_NOT_FOUND", HttpStatus.NOT_FOUND, "Objectiu no trobat"));
        if (!goal.getChild().getId().equals(childId)) {
            throw new DomainException("ACCESS_DENIED", HttpStatus.FORBIDDEN, "Aquest objectiu no és d'aquest fill");
        }
        return goal;
    }

    private BigDecimal allocationOrZero(CreateSavingsGoalRequest request) {
        return request.allocationPercentage() != null ? request.allocationPercentage() : BigDecimal.ZERO;
    }

    private void requireAllocationFits(ChildProfile child, BigDecimal newAllocation, UUID excludingGoalId) {
        BigDecimal spendingPercentage = allowanceRuleService.resolveSpendingPercentage(child);
        BigDecimal othersSum = savingsGoalRepository.findByChildIdAndStatus(child.getId(), SavingsGoalStatus.ACTIVE).stream()
                .filter(g -> excludingGoalId == null || !g.getId().equals(excludingGoalId))
                .map(SavingsGoal::getAllocationPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (othersSum.add(newAllocation).compareTo(spendingPercentage) > 0) {
            throw new DomainException("GOAL_PERCENTAGE_EXCEEDS_AVAILABLE", HttpStatus.BAD_REQUEST,
                    "El percentatge de l'objectiu supera el marge disponible de \"per gastar\"");
        }
    }

    private SavingsGoalResponse toResponse(SavingsGoal goal) {
        BigDecimal currentAmount = moneyTransactionRepository.goalProgress(goal.getId());
        return SavingsGoalResponse.from(goal, currentAmount);
    }
}
