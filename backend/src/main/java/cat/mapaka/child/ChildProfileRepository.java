package cat.mapaka.child;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChildProfileRepository extends JpaRepository<ChildProfile, UUID> {

    Optional<ChildProfile> findByUserId(UUID userId);

    @Query("SELECT c FROM ChildProfile c WHERE c.user.family.id = :familyId")
    List<ChildProfile> findAllByFamilyId(UUID familyId);

    @Query("SELECT c FROM ChildProfile c JOIN FETCH c.user WHERE c.user.family.id = :familyId AND c.active = true")
    List<ChildProfile> findAllActiveByFamilyIdFetchUser(UUID familyId);
}
