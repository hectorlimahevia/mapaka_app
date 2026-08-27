package cat.mapaka.savings;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ContributeToGoalRequest(@NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
}
