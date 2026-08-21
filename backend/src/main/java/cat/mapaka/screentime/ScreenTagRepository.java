package cat.mapaka.screentime;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScreenTagRepository extends JpaRepository<ScreenTag, UUID> {

    Optional<ScreenTag> findByToken(String token);

    List<ScreenTag> findByFamilyIdOrderByCreatedAtDesc(UUID familyId);
}
