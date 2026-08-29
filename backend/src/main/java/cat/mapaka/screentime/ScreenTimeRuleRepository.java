package cat.mapaka.screentime;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ScreenTimeRuleRepository extends JpaRepository<ScreenTimeRule, UUID> {

    Optional<ScreenTimeRule> findByChildIdAndWeekdayIsNullAndActiveTrue(UUID childId);
}
