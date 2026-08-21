package cat.mapaka.user;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Deriva un username intern (mai visible a l'usuari, només fet servir per identificar el
 * login dins de la família) a partir del nom mostrat — ni l'alta de PARENT ni la de CHILD
 * (Prompt 6) demanen un username explícit, només un nom.
 */
@Component
public class UsernameAllocator {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");

    private final UserRepository userRepository;

    public UsernameAllocator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String allocate(UUID familyId, String displayName) {
        String base = slugify(displayName);
        if (userRepository.findByFamilyIdAndUsername(familyId, base).isEmpty()) {
            return base;
        }
        var existing = userRepository.findAllByFamilyIdAndUsernameStartingWith(familyId, base).stream()
                .map(User::getUsername)
                .toList();
        int suffix = 2;
        while (existing.contains(base + suffix)) {
            suffix++;
        }
        return base + suffix;
    }

    private String slugify(String displayName) {
        String normalized = DIACRITICS.matcher(Normalizer.normalize(displayName, Normalizer.Form.NFD)).replaceAll("");
        String slug = NON_ALPHANUMERIC.matcher(normalized.toLowerCase(Locale.ROOT)).replaceAll("");
        return slug.isBlank() ? "usuari" : slug;
    }
}
