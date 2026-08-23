package cat.mapaka.allowance;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GeneralAllowanceRuleRequest(
        @NotNull @Min(0) Integer minAge,
        @NotNull @Min(0) Integer maxAge,
        @NotNull BigDecimal monthlyAmount,
        @NotNull BigDecimal spendingPercentage,
        @NotNull BigDecimal savingsPercentage) {
}
