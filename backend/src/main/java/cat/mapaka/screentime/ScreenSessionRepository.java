package cat.mapaka.screentime;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ScreenSessionRepository extends JpaRepository<ScreenSession, UUID> {

    Optional<ScreenSession> findByScreenTagIdAndStatus(UUID screenTagId, ScreenSessionStatus status);
}
