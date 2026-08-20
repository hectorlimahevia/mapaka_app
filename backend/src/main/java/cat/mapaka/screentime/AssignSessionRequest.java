package cat.mapaka.screentime;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record AssignSessionRequest(@NotEmpty List<UUID> childIds) {
}
