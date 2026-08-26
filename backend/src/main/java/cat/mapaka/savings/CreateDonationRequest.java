package cat.mapaka.savings;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateDonationRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        String donorName,
        String message) {
}
