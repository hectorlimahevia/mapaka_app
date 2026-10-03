package cat.mapaka.adjustment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** destination és opcional (null = RULE, el comportament de sempre); spendingAmount només
 * s'usa amb CUSTOM i és la part que va a gastar — la resta va a estalvi. */
public record MoneyAdjustmentRequest(
        @NotNull AdjustmentType type,
        @NotNull BigDecimal amount,
        @NotBlank String reason,
        AdjustmentDestination destination,
        BigDecimal spendingAmount) {
}
