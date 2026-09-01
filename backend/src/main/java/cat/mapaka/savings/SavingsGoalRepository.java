package cat.mapaka.savings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, UUID> {

    List<SavingsGoal> findByChildId(UUID childId);

    boolean existsByChildId(UUID childId);

    List<SavingsGoal> findByChildIdAndStatus(UUID childId, SavingsGoalStatus status);

    List<SavingsGoal> findByChildIdAndStatusNot(UUID childId, SavingsGoalStatus status);
}
