package cat.mapaka.savings;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * currentAmount és el saldo total de la butxaca d'estalvi del fill (wallet SAVINGS), no un
 * import reservat per objectiu — l'esquema no vincula moviments a un objectiu concret,
 * així que cada objectiu mostra el progrés contra el mateix total d'estalvi compartit.
 */
public record SavingsGoalResponse(
        UUID id, String name, BigDecimal targetAmount, BigDecimal currentAmount, String imageUrl, SavingsGoalStatus status) {

    public static SavingsGoalResponse from(SavingsGoal goal, BigDecimal currentAmount) {
        return new SavingsGoalResponse(
                goal.getId(), goal.getName(), goal.getTargetAmount(), currentAmount, goal.getImageUrl(), goal.getStatus());
    }
}
