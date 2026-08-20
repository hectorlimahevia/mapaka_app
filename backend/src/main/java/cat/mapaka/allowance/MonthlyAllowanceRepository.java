package cat.mapaka.allowance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MonthlyAllowanceRepository extends JpaRepository<MonthlyAllowance, UUID> {

    Optional<MonthlyAllowance> findByChildIdAndYearAndMonth(UUID childId, int year, int month);
}
