package cat.mapaka.screentime;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScreenSessionParticipantRepository extends JpaRepository<ScreenSessionParticipant, UUID> {

    List<ScreenSessionParticipant> findBySessionId(UUID sessionId);
}
