package cat.mapaka.screentime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ScreenTimeRuleUpdateRequest(@NotNull @Min(0) Integer baseMinutes) {
}
