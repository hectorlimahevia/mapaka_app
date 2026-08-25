package cat.mapaka.child;

import java.util.Set;

/** Paleta tancada de colors per a l'avatar d'un fill (Prompt 15) — mateixos 9 tons al
 * backend i al selector del frontend. Cap to blanc ni molt clar és una opció vàlida,
 * perquè la icona blanca de l'avatar sempre hi tingui contrast. Reflectida també a la
 * restricció CHECK de child_profiles.color_theme (migració V12). */
public final class ChildColorPalette {

    public static final Set<String> VALID_COLORS = Set.of(
            "#6C4DFF", "#FF5D8F", "#FFC93C", "#2ECC71", "#3AA0FF",
            "#F5765B", "#8E44AD", "#16A085", "#E91E63");

    private ChildColorPalette() {
    }

    public static boolean isValid(String colorTheme) {
        return colorTheme == null || VALID_COLORS.contains(colorTheme);
    }
}
