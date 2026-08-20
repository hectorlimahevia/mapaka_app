package cat.mapaka.task;

import cat.mapaka.child.ChildAccessService;
import cat.mapaka.child.ChildProfile;
import cat.mapaka.child.ChildProfileRepository;
import cat.mapaka.common.DomainException;
import cat.mapaka.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
public class TaskController {

    private final ChildAccessService childAccessService;
    private final ChildProfileRepository childProfileRepository;
    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskService taskService;

    public TaskController(
            ChildAccessService childAccessService,
            ChildProfileRepository childProfileRepository,
            TaskRepository taskRepository,
            TaskAssignmentRepository taskAssignmentRepository,
            TaskService taskService) {
        this.childAccessService = childAccessService;
        this.childProfileRepository = childProfileRepository;
        this.taskRepository = taskRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.taskService = taskService;
    }

    @GetMapping("/api/children/{id}/tasks")
    public List<ChildTaskResponse> tasks(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        ChildProfile child = childAccessService.requireAccess(id, user);
        return taskService.getTasksFor(child);
    }

    /** Nomes el propi fill autenticat pot marcar una tasca com a feta (mai un altre rol). */
    @PreAuthorize("hasRole('CHILD')")
    @PostMapping("/api/tasks/{taskId}/complete")
    public void complete(@PathVariable UUID taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        ChildProfile child = childProfileRepository.findByIdFetchUserAndFamily(user.childId())
                .orElseThrow(() -> new DomainException("CHILD_NOT_FOUND", HttpStatus.NOT_FOUND, "Fill no trobat"));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new DomainException("TASK_NOT_FOUND", HttpStatus.NOT_FOUND, "Tasca no trobada"));

        boolean assigned = taskAssignmentRepository.findByTaskIdAndChildId(taskId, child.getId())
                .map(TaskAssignment::isActive)
                .orElse(false);
        if (!assigned) {
            throw new DomainException("TASK_NOT_ASSIGNED", HttpStatus.FORBIDDEN, "Aquesta tasca no està assignada a aquest fill");
        }

        ZoneId familyZone = ZoneId.of(child.getUser().getFamily().getTimezone());
        taskService.completeTask(task, child, familyZone);
    }
}
