package cat.mapaka.screentime;

import cat.mapaka.family.Family;
import cat.mapaka.family.FamilyAccessService;
import cat.mapaka.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/**
 * Gestió d'etiquetes NFC per al PARENT (mapaka_prompts_code.md Prompt 9, funcionalitat
 * opcional d'escriptura des de l'app): registrar una etiqueta nova al backend és el pas
 * previ a escriure-la físicament amb el plugin natiu — mai al revés, perquè un toc a una
 * etiqueta desconeguda ha de fallar de forma predictible (SCREEN_TAG_NOT_FOUND).
 */
@RestController
@PreAuthorize("hasRole('PARENT')")
public class ScreenTagAdminController {

    private static final String TOKEN_ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final FamilyAccessService familyAccessService;
    private final ScreenTagRepository screenTagRepository;

    public ScreenTagAdminController(FamilyAccessService familyAccessService, ScreenTagRepository screenTagRepository) {
        this.familyAccessService = familyAccessService;
        this.screenTagRepository = screenTagRepository;
    }

    /**
     * L'enllaç d'una etiqueta NFC l'ha d'obrir qualsevol dispositiu que la toqui — mai pot
     * dependre de l'origen "mapaka.cors.allowed-origin" (pensat per validar CORS, que a
     * l'app empaquetada amb Capacitor val "https://localhost", l'origen intern del WebView,
     * no una URL pública). Es deriva sempre de la petició real que arriba al backend
     * (mateix host que ja fa servir qualsevol client, web o Capacitor, per parlar amb
     * l'API), de manera que funciona igual en dev i en producció sense cap configuració
     * addicional.
     */
    private String publicBaseUrl(HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(null)
                .replaceQuery(null)
                .build()
                .toUriString();
    }

    @GetMapping("/api/families/{id}/screen-tags")
    public List<ScreenTagResponse> list(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user, HttpServletRequest request) {
        familyAccessService.requireParentAccess(id, user);
        String baseUrl = publicBaseUrl(request);
        return screenTagRepository.findByFamilyIdOrderByCreatedAtDesc(id).stream()
                .map(tag -> ScreenTagResponse.from(tag, baseUrl))
                .toList();
    }

    @PostMapping("/api/families/{id}/screen-tags")
    public ScreenTagResponse create(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user, HttpServletRequest request) {
        Family family = familyAccessService.requireParentAccess(id, user);

        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                ScreenTag tag = screenTagRepository.save(ScreenTag.builder()
                        .family(family).token(generateToken()).active(true).build());
                return ScreenTagResponse.from(tag, publicBaseUrl(request));
            } catch (DataIntegrityViolationException tokenCollision) {
                // Token ja existent (extremadament improbable) — reintenta amb un altre.
            }
        }
        throw new IllegalStateException("No s'ha pogut generar un token únic per a l'etiqueta NFC");
    }

    /** Alfabet sense caràcters ambigus (0/O, 1/l/I) — el token no s'escriu mai a mà, però la
     *  depuració i els logs es llegeixen millor sense ambigüitats. */
    private String generateToken() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(TOKEN_ALPHABET.charAt(RANDOM.nextInt(TOKEN_ALPHABET.length())));
        }
        return sb.toString();
    }
}
