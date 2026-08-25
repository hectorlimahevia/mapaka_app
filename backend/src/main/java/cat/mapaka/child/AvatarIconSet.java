package cat.mapaka.child;

import java.util.Set;

/** Set tancat d'icones per a l'avatar d'un fill (Prompt 15) — icones d'animals en estil
 * contorn de Phosphor Icons (MIT), perquè Font Awesome Classic Regular exigeix llicència
 * Pro i el projecte es manté a cost zero. Mateixes claus al backend i al selector del
 * frontend. `null` és sempre vàlid i vol dir "mostra la inicial del nom". */
public final class AvatarIconSet {

    public static final Set<String> VALID_ICONS = Set.of(
            "cat", "dog", "bird", "fish", "rabbit", "horse", "butterfly", "cow");

    private AvatarIconSet() {
    }

    public static boolean isValid(String icon) {
        return icon == null || VALID_ICONS.contains(icon);
    }
}
