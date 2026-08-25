package cat.mapaka.child;

import java.util.Set;

/** Set tancat d'icones per a l'avatar d'un fill (Prompt 15) — mateixes claus al backend i
 * al selector del frontend (línia blanca sobre el color triat). `null` és sempre vàlid i
 * vol dir "mostra la inicial del nom en comptes d'una icona". */
public final class AvatarIconSet {

    public static final Set<String> VALID_ICONS = Set.of(
            "star", "cross", "circle", "house", "arrow", "heart");

    private AvatarIconSet() {
    }

    public static boolean isValid(String icon) {
        return icon == null || VALID_ICONS.contains(icon);
    }
}
