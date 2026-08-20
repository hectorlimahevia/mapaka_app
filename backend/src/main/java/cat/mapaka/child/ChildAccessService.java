package cat.mapaka.child;

import cat.mapaka.common.DomainException;
import cat.mapaka.security.AuthenticatedUser;
import cat.mapaka.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Control d'autorització per a dades d'un fill (Família+.pdf secció 41): un CHILD només
 * pot accedir a les seves pròpies dades; un PARENT pot accedir a qualsevol fill de la
 * seva família. Mai es confia en el childId enviat pel frontend sense verificar-ho.
 */
@Service
public class ChildAccessService {

    private final ChildProfileRepository childProfileRepository;

    public ChildAccessService(ChildProfileRepository childProfileRepository) {
        this.childProfileRepository = childProfileRepository;
    }

    public ChildProfile requireAccess(UUID childId, AuthenticatedUser requester) {
        ChildProfile child = childProfileRepository.findByIdFetchUserAndFamily(childId)
                .orElseThrow(() -> new DomainException("CHILD_NOT_FOUND", HttpStatus.NOT_FOUND, "Fill no trobat"));

        UUID childFamilyId = child.getUser().getFamily().getId();
        if (!childFamilyId.equals(requester.familyId())) {
            throw new DomainException("ACCESS_DENIED", HttpStatus.FORBIDDEN, "No pots accedir a un fill d'una altra família");
        }
        if (requester.role() == UserRole.CHILD && !child.getId().equals(requester.childId())) {
            throw new DomainException("ACCESS_DENIED", HttpStatus.FORBIDDEN, "Un fill només pot consultar les seves pròpies dades");
        }
        return child;
    }
}
