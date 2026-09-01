package cat.mapaka.savings;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateSavingsGoalRequest(
        @NotBlank String name,
        @NotNull @DecimalMin(value = "0.01") BigDecimal targetAmount,
        /** Opcional — omès o null equival a 0 (cap repartiment cap a aquest objectiu). */
        @DecimalMin(value = "0") @DecimalMax(value = "100") BigDecimal allocationPercentage,
        /** Opcional — ids de germans convidats a compartir aquest objectiu amb les mateixes
         * condicions. Buit o null equival a "no compartir". */
        List<UUID> shareWithChildIds) {
}
