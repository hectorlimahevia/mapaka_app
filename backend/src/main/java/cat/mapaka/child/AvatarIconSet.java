package cat.mapaka.child;

import java.util.Set;

/** Set tancat d'icones per a l'avatar d'un fill (Prompt 15 + ajust posterior) — icones en
 * estil contorn de Phosphor Icons (MIT), perquè Font Awesome Classic Regular exigeix
 * llicència Pro i el projecte es manté a cost zero. Mateixes claus al backend i al
 * selector del frontend (agrupades per categoria a `avatarIcons.ts`: animals, esports,
 * vehicles, fantasia — 12 de cada). `null` és sempre vàlid i vol dir "mostra la inicial
 * del nom". */
public final class AvatarIconSet {

    public static final Set<String> VALID_ICONS = Set.of(
            // Animals
            "cat", "dog", "bird", "fish", "rabbit", "horse", "butterfly", "cow",
            "bug", "paw-print", "feather", "shrimp",
            // Esports
            "soccer-ball", "basketball", "football", "tennis-ball", "baseball", "volleyball",
            "bicycle", "medal", "trophy", "boxing-glove", "ping-pong", "golf",
            // Vehicles
            "car", "rocket", "airplane", "train", "bus", "boat",
            "motorcycle", "tractor", "van", "sailboat", "truck", "balloon",
            // Fantasia
            "star", "heart", "crown", "magic-wand", "rainbow", "ghost",
            "robot", "alien", "sparkle", "planet", "moon-stars", "shooting-star");

    private AvatarIconSet() {
    }

    public static boolean isValid(String icon) {
        return icon == null || VALID_ICONS.contains(icon);
    }
}
