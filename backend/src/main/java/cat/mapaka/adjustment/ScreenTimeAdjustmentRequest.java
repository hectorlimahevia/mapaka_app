package cat.mapaka.adjustment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ScreenTimeAdjustmentRequest(
        @NotNull AdjustmentType type,
        int minutes,
        @NotBlank String reason) {
}
