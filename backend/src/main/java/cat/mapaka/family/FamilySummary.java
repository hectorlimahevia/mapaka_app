package cat.mapaka.family;

import java.util.UUID;

/** Només per a la cerca de família a la pantalla de login del fill (secció 39) — mai dades
 * sensibles. `familyCode` no és secret (a diferència del recovery code): es mostra sempre
 * perquè, si hi ha diverses famílies amb el mateix `name`, l'usuari pugui distingir la seva. */
public record FamilySummary(UUID id, String name, String familyCode) {

    public static FamilySummary from(Family family) {
        return new FamilySummary(family.getId(), family.getName(), family.getFamilyCode());
    }
}
