package cat.mapaka.family;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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
        List<GoalAllocationSummary> goals) {
}
