package cat.mapaka.allowance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AllowanceRuleUpdateRequest(
        @NotNull @DecimalMin("0") BigDecimal monthlyAmount,
        @NotNull @DecimalMin("0") BigDecimal spendingPercentage,
        @NotNull @DecimalMin("0") BigDecimal savingsPercentage) {
}
