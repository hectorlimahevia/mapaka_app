package cat.mapaka.family;

import java.util.UUID;

/** Només per a la cerca de família a la pantalla de login del fill (secció 39) — mai dades sensibles. */
public record FamilySummary(UUID id, String name) {

    public static FamilySummary from(Family family) {
        return new FamilySummary(family.getId(), family.getName());
    }
}
