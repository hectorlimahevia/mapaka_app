package cat.mapaka.screentime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ScreenSessionParticipantRepository extends JpaRepository<ScreenSessionParticipant, UUID> {

    List<ScreenSessionParticipant> findBySessionId(UUID sessionId);

    boolean existsByChildId(UUID childId);

    @Query("""
        SELECT p FROM ScreenSessionParticipant p JOIN FETCH p.child
        WHERE p.child.user.family.id = :familyId AND p.resultingBalanceNegative = true
        ORDER BY p.createdAt DESC
        """)
    List<ScreenSessionParticipant> findNegativeBalanceByFamilyId(UUID familyId);
}
