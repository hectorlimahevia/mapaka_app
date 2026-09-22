package cat.mapaka.auth;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * Login de PARENT o CHILD (secció 39): familyId + username + PIN (viatja al camp password).
 */
public record LoginRequest(UUID familyId, @NotBlank String username, @NotBlank String password) {
}
