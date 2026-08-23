package cat.mapaka.settlement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonthlySettlementRepository extends JpaRepository<MonthlySettlement, UUID> {

    Optional<MonthlySettlement> findByChildIdAndYearAndMonth(UUID childId, int year, int month);

    @Query("""
        SELECT s FROM MonthlySettlement s JOIN FETCH s.child c JOIN FETCH c.user u
        WHERE u.family.id = :familyId ORDER BY s.year DESC, s.month DESC
        """)
    List<MonthlySettlement> findByFamilyId(UUID familyId);

    @Query("SELECT s FROM MonthlySettlement s JOIN FETCH s.child c JOIN FETCH c.user u JOIN FETCH u.family WHERE s.id = :id")
    Optional<MonthlySettlement> findByIdFetchFamily(UUID id);
}
