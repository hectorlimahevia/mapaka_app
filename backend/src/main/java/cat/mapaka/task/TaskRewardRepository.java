package cat.mapaka.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TaskRewardRepository extends JpaRepository<TaskReward, UUID> {

    Optional<TaskReward> findByTaskIdAndActiveTrue(UUID taskId);
}
