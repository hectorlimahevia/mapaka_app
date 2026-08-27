package cat.mapaka.child;

import java.util.Set;

/** Set tancat d'icones per a l'avatar d'un fill (Prompt 15 + ajust posterior) — icones en
 * estil contorn de Phosphor Icons (MIT), perquè Font Awesome Classic Regular exigeix
 * llicència Pro i el projecte es manté a cost zero. Mateixes claus al backend i al
 * selector del frontend (agrupades per categoria a `avatarIcons.ts`: animals, esports,
 * vehicles, fantasia — 15 de cada, perquè la graella ompli 4 files de 4 sense deixar-ne
 * una a mitges). `null` és sempre vàlid i vol dir "mostra la inicial del nom". */
public final class AvatarIconSet {

    public static final Set<String> VALID_ICONS = Set.of(
            // Animals
            "cat", "dog", "bird", "fish", "rabbit", "horse", "butterfly", "cow",
            "bug", "paw-print", "feather", "shrimp", "bone", "egg", "leaf",
            // Esports
            "soccer-ball", "basketball", "football", "tennis-ball", "baseball", "volleyball",
            "bicycle", "medal", "trophy", "boxing-glove", "ping-pong", "golf",
            "barbell", "racquet", "hockey",
            // Vehicles
            "car", "rocket", "airplane", "train", "bus", "boat",
            "motorcycle", "tractor", "van", "sailboat", "truck", "balloon",
            "jeep", "scooter", "cable-car",
            // Fantasia
            "star", "heart", "crown", "magic-wand", "rainbow", "ghost",
            "robot", "alien", "sparkle", "planet", "moon-stars", "shooting-star",
            "gift", "lightning", "confetti");

    private AvatarIconSet() {
    }

    public static boolean isValid(String icon) {
        return icon == null || VALID_ICONS.contains(icon);
    }
}
