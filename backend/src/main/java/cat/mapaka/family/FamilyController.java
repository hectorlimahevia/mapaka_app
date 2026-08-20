package cat.mapaka.family;

import cat.mapaka.child.ChildProfileRepository;
import cat.mapaka.child.ChildSummary;
import cat.mapaka.common.DomainException;
import cat.mapaka.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class FamilyController {

    private final ChildProfileRepository childProfileRepository;

    public FamilyController(ChildProfileRepository childProfileRepository) {
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
}
