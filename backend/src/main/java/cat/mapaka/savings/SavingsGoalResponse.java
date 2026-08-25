package cat.mapaka.savings;

import java.math.BigDecimal;
import java.util.UUID;

/** currentAmount és el progrés propi d'aquest objectiu (repartiment + donacions, wallet
 * GOAL vinculat al seu id) — mai el total d'estalvi compartit del fill (Prompt 15). */
public record SavingsGoalResponse(
        UUID id, String name, BigDecimal targetAmount, BigDecimal allocationPercentage,
        BigDecimal currentAmount, String imageUrl, SavingsGoalStatus status) {

    public static SavingsGoalResponse from(SavingsGoal goal, BigDecimal currentAmount) {
        return new SavingsGoalResponse(
                goal.getId(), goal.getName(), goal.getTargetAmount(), goal.getAllocationPercentage(),
                currentAmount, goal.getImageUrl(), goal.getStatus());
    }
}
