package cat.mapaka.auth;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * Login d'adult (Família+.pdf 38): email + password.
 * Login de fill (secció 39): familyId + username + PIN (viatja al camp password).
 */
public record LoginRequest(String email, UUID familyId, String username, @NotBlank String password) {
}
