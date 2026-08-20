package cat.mapaka.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, UUID> {

    List<TaskAssignment> findByChildId(UUID childId);

    Optional<TaskAssignment> findByTaskIdAndChildId(UUID taskId, UUID childId);
}
