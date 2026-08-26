package cat.mapaka.money;

import java.math.BigDecimal;

/** `spendingPercentage`/`allocatedGoalPercentage` alimenten la previsualització en viu del
 * formulari d'objectius (Prompt 15, secció 8.1): el marge disponible per a un objectiu nou
 * és `spendingPercentage - allocatedGoalPercentage`. */
public record WalletResponse(
        BigDecimal spendingBalance,
        BigDecimal savingsBalance,
        BigDecimal total,
        BigDecimal spendingPercentage,
        BigDecimal allocatedGoalPercentage) {
}
