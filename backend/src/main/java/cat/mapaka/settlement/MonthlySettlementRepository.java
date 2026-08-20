package cat.mapaka.settlement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MonthlySettlementRepository extends JpaRepository<MonthlySettlement, UUID> {

    Optional<MonthlySettlement> findByChildIdAndYearAndMonth(UUID childId, int year, int month);
}
