package cat.mapaka.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskCompletionRepository extends JpaRepository<TaskCompletion, UUID> {

    List<TaskCompletion> findByChildIdAndStatus(UUID childId, TaskCompletionStatus status);

    List<TaskCompletion> findByTaskId(UUID taskId);

    List<TaskCompletion> findByTaskIdAndChildIdOrderByCompletedAtDesc(UUID taskId, UUID childId);
}
