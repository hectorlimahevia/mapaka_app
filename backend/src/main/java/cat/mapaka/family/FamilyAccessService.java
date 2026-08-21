package cat.mapaka.family;

import cat.mapaka.common.DomainException;
import cat.mapaka.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Un PARENT només administra la seva pròpia família (Família+.pdf secció 41). */
@Service
public class FamilyAccessService {

    private final FamilyRepository familyRepository;

    public FamilyAccessService(FamilyRepository familyRepository) {
        this.familyRepository = familyRepository;
    }

    public Family requireParentAccess(UUID familyId, AuthenticatedUser requester) {
        if (!familyId.equals(requester.familyId())) {
            throw new DomainException("ACCESS_DENIED", HttpStatus.FORBIDDEN, "No pots administrar una altra família");
        }
        return familyRepository.findById(familyId)
                .orElseThrow(() -> new DomainException("FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND, "Família no trobada"));
    }
}
