package cat.mapaka.task;

import cat.mapaka.child.ChildAccessService;
import cat.mapaka.child.ChildProfile;
import cat.mapaka.common.DomainException;
import cat.mapaka.family.Family;
import cat.mapaka.family.FamilyAccessService;
import cat.mapaka.security.AuthenticatedUser;
import cat.mapaka.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('PARENT')")
public class TaskManagementController {

    private final TaskManagementService taskManagementService;
    private final TaskPenaltyService taskPenaltyService;
    private final TaskRepository taskRepository;
    private final ChildAccessService childAccessService;
    private final FamilyAccessService familyAccessService;
    private final UserRepository userRepository;

    public TaskManagementController(
            TaskManagementService taskManagementService,
            TaskPenaltyService taskPenaltyService,
            TaskRepository taskRepository,
            ChildAccessService childAccessService,
            FamilyAccessService familyAccessService,
            UserRepository userRepository) {
        this.taskManagementService = taskManagementService;
        this.taskPenaltyService = taskPenaltyService;
        this.taskRepository = taskRepository;
        this.childAccessService = childAccessService;
        this.familyAccessService = familyAccessService;
        this.userRepository = userRepository;
    }

    @GetMapping("/api/tasks")
    public List<TaskManagementResponse> list(
            @RequestParam(required = false) TaskType type,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) UUID childId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return taskManagementService.list(user.familyId(), type, active, childId);
    }

    @PostMapping("/api/tasks")
    public ResponseEntity<TaskManagementResponse> create(
            @Valid @RequestBody CreateTaskRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        Family family = familyAccessService.requireParentAccess(user.familyId(), user);
        var createdBy = userRepository.getReferenceById(user.userId());
        return ResponseEntity.ok(taskManagementService.create(family, createdBy, request));
    }

    @PatchMapping("/api/tasks/{id}")
    public TaskManagementResponse update(
            @PathVariable UUID id, @Valid @RequestBody CreateTaskRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        Task task = requireTaskInFamily(id, user);
        return taskManagementService.update(task, request);
    }

    @DeleteMapping("/api/tasks/{id}")
    public void deactivate(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        Task task = requireTaskInFamily(id, user);
        taskManagementService.deactivate(task);
    }

    @GetMapping("/api/tasks/incomplete")
    public List<IncompleteTaskResponse> incomplete(
            @RequestParam(required = false) Instant asOf, @AuthenticationPrincipal AuthenticatedUser user) {
        return taskPenaltyService.incomplete(user.familyId(), asOf != null ? asOf : Instant.now());
    }

    @PostMapping("/api/tasks/{taskId}/children/{childId}/apply-penalty")
    public void applyPenalty(
            @PathVariable UUID taskId, @PathVariable UUID childId, @AuthenticationPrincipal AuthenticatedUser user) {
        Task task = requireTaskInFamily(taskId, user);
        ChildProfile child = childAccessService.requireAccess(childId, user);
        taskPenaltyService.applyPenalty(task, child, user.userId());
    }

    private Task requireTaskInFamily(UUID taskId, AuthenticatedUser user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new DomainException("TASK_NOT_FOUND", HttpStatus.NOT_FOUND, "Tasca no trobada"));
        if (!task.getFamily().getId().equals(user.familyId())) {
            throw new DomainException("ACCESS_DENIED", HttpStatus.FORBIDDEN, "No pots gestionar una tasca d'una altra família");
        }
        return task;
    }
}
