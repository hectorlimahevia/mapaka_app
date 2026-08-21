package cat.mapaka.family;

import java.math.BigDecimal;
import java.util.UUID;

public record ChildFamilySummary(
        UUID childId, String displayName, String avatar, BigDecimal spendingBalance, BigDecimal savingsBalance, long pendingApprovalsCount) {
}
