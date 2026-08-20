package cat.mapaka.screentime;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScreenTimeRuleRepository extends JpaRepository<ScreenTimeRule, UUID> {

    List<ScreenTimeRule> findByChildIdAndActiveTrue(UUID childId);
}
