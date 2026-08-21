package cat.mapaka.screentime;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScreenTimeRuleRepository extends JpaRepository<ScreenTimeRule, UUID> {

    List<ScreenTimeRule> findByChildIdAndActiveTrue(UUID childId);

    Optional<ScreenTimeRule> findByChildIdAndWeekdayIsNullAndActiveTrue(UUID childId);
}
