package cat.mapaka.auth;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record RecoverRequest(UUID familyId, @NotBlank String recoveryCode) {
}
