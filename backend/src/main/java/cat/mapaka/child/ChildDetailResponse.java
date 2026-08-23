package cat.mapaka.child;

import java.math.BigDecimal;
import java.util.UUID;

public record ChildDetailResponse(
        UUID childId,
        String displayName,
        String avatar,
        int age,
        boolean hasCustomAllowance,
        BigDecimal allowanceMonthlyAmount,
        BigDecimal allowanceSpendingPercentage,
        BigDecimal allowanceSavingsPercentage,
        Integer screenBaseMinutes) {
}
