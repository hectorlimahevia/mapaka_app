package cat.mapaka.family;

import cat.mapaka.child.ChildLoginProfile;
import cat.mapaka.child.ChildProfileRepository;
import cat.mapaka.child.ChildSummary;
import cat.mapaka.common.DomainException;
import cat.mapaka.security.AuthenticatedUser;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class FamilyController {

    private final FamilyRepository familyRepository;
    private final ChildProfileRepository childProfileRepository;

    public FamilyController(FamilyRepository familyRepository, ChildProfileRepository childProfileRepository) {
        this.familyRepository = familyRepository;
        this.childProfileRepository = childProfileRepository;
    }

    @GetMapping("/api/families/{id}/children")
    public List<ChildSummary> children(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        // Un usuari només pot consultar la seva pròpia família (Família+.pdf secció 41).
        if (!id.equals(user.familyId())) {
            throw new DomainException("ACCESS_DENIED", HttpStatus.FORBIDDEN, "No pots consultar una altra família");
        }
        return childProfileRepository.findAllByFamilyId(id).stream()
                .map(ChildSummary::from)
                .toList();
    }

    /**
     * Cerca pública de família pel login infantil (secció 39: "Família → Selecciona perfil").
     * Només retorna id + nom — mai dades de fills ni saldos.
     */
    @GetMapping("/api/families/lookup")
    public List<FamilySummary> lookup(@RequestParam String q) {
        if (q.isBlank()) {
            return List.of();
        }
        return familyRepository.findByActiveTrueAndNameContainingIgnoreCase(q, Limit.of(10)).stream()
                .map(FamilySummary::from)
                .toList();
    }

    /**
     * Perfils per al selector "Qui ets?" un cop triada la família — públic i intencionadament
     * mínim (nom, avatar, username), sense cap dada financera ni de tasques.
     */
    @GetMapping("/api/families/{id}/login-profiles")
    public List<ChildLoginProfile> loginProfiles(@PathVariable UUID id) {
        return childProfileRepository.findAllActiveByFamilyIdFetchUser(id).stream()
                .map(ChildLoginProfile::from)
                .toList();
    }
}
