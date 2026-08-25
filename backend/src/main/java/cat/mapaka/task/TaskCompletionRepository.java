package cat.mapaka.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TaskCompletionRepository extends JpaRepository<TaskCompletion, UUID> {

    List<TaskCompletion> findByChildIdAndStatus(UUID childId, TaskCompletionStatus status);

    List<TaskCompletion> findByTaskId(UUID taskId);

    List<TaskCompletion> findByTaskIdAndChildIdOrderByCompletedAtDesc(UUID taskId, UUID childId);

    long countByChildIdAndStatus(UUID childId, TaskCompletionStatus status);

    @Query("""
        SELECT c FROM TaskCompletion c JOIN FETCH c.child JOIN FETCH c.task
        WHERE c.task.family.id = :familyId AND c.status = :status
        ORDER BY c.completedAt ASC
        """)
    List<TaskCompletion> findByFamilyIdAndStatusOrderByCompletedAtAsc(UUID familyId, TaskCompletionStatus status);

    @Query("""
        SELECT c FROM TaskCompletion c
        JOIN FETCH c.child ch JOIN FETCH ch.user u JOIN FETCH u.family
        JOIN FETCH c.task
        WHERE c.id = :id
        """)
    java.util.Optional<TaskCompletion> findByIdFetchChildAndTask(UUID id);

    @Query("""
        SELECT c FROM TaskCompletion c
        JOIN FETCH c.child ch JOIN FETCH ch.user u JOIN FETCH u.family
        JOIN FETCH c.task
        WHERE c.completionGroupId = :completionGroupId
        """)
    List<TaskCompletion> findByCompletionGroupIdFetchChildAndTask(UUID completionGroupId);
}
