package cat.mapaka.family;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** screenMinutes és el saldo del monedero de temps de pantalla (pot ser negatiu); és null
 * quan el temps de pantalla està desactivat per a aquest fill. */
public record ChildFamilySummary(
        UUID childId,
        String displayName,
        String avatar,
        String avatarColor,
        String avatarIcon,
        BigDecimal spendingBalance,
        BigDecimal savingsBalance,
        BigDecimal totalBalance,
        long pendingApprovalsCount,
        List<GoalAllocationSummary> goals,
        Integer screenMinutes) {
}
