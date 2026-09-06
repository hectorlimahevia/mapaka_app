package cat.mapaka.family;

import cat.mapaka.auth.AuthResponse;

/** El codi de recuperació només viatja en aquesta resposta — no es torna a poder consultar mai
 * més. El codi de família, en canvi, no és secret: també es pot tornar a consultar sempre des
 * de Configuració (FamilySettingsResponse). */
public record FamilyRegisterResponse(AuthResponse auth, String recoveryCode, String familyCode) {
}
