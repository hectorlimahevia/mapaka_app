package cat.mapaka.family;

import cat.mapaka.auth.AuthResponse;

/** El codi de recuperació només viatja en aquesta resposta — no es torna a poder consultar mai més. */
public record FamilyRegisterResponse(AuthResponse auth, String recoveryCode) {
}
