package cat.mapaka.adjustment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MoneyAdjustmentRequest(
        @NotNull AdjustmentType type,
        @NotNull BigDecimal amount,
        @NotNull BigDecimal savingsAmount,
        @NotBlank String reason) {
}
