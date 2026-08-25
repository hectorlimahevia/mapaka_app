package cat.mapaka.task;

import cat.mapaka.child.ChildProfile;
import cat.mapaka.common.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Tasques vistes pel fill (Família+.pdf secció 2.1 i 12-13). Marcar una tasca com a feta
 * només crea un/uns TaskCompletion PENDING amb un snapshot de la recompensa vigent — mai
 * toca els ledgers directament (això és feina de l'aprovació del PARENT, ApprovalService).
 */
@Service
public class TaskService {

    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskRewardRepository taskRewardRepository;
    private final TaskCompletionRepository taskCompletionRepository;
    private final TaskRepository taskRepository;

    public TaskService(
            TaskAssignmentRepository taskAssignmentRepository,
            TaskRewardRepository taskRewardRepository,
            TaskCompletionRepository taskCompletionRepository,
            TaskRepository taskRepository) {
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.taskRewardRepository = taskRewardRepository;
        this.taskCompletionRepository = taskCompletionRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<ChildTaskResponse> getTasksFor(ChildProfile child) {
        ZoneId familyZone = ZoneId.of(child.getUser().getFamily().getTimezone());
        UUID familyId = child.getUser().getFamily().getId();

        List<Task> assignedTasks = taskAssignmentRepository.findByChildId(child.getId()).stream()
                .filter(TaskAssignment::isActive)
                .map(TaskAssignment::getTask)
                .filter(Task::isActive)
                .toList();

        // Una tasca Extra sense assignació és visible per a tots els fills de la família
        // (Prompt 15) — mai per als fills que ja la tenen assignada individualment (v1).
        List<Task> openExtraTasks = taskRepository.findByFamilyId(familyId).stream()
                .filter(Task::isActive)
                .filter(t -> t.getTaskType() == TaskType.EXTRA)
                .filter(t -> taskAssignmentRepository.findByTaskId(t.getId()).stream().noneMatch(TaskAssignment::isActive))
                .toList();

        List<Task> allTasks = new ArrayList<>(assignedTasks);
        for (Task t : openExtraTasks) {
            if (allTasks.stream().noneMatch(existing -> existing.getId().equals(t.getId()))) {
                allTasks.add(t);
            }
        }

        return allTasks.stream().map(task -> toResponse(task, child, familyZone)).toList();
    }

    /** Finalització individual (tasques amb assignació fixa, típicament Responsabilitat).
     * Genera igualment un completion_group_id propi perquè l'aprovació sempre tracti un
     * grup, encara que sigui d'un sol participant (Prompt 15). */
    @Transactional
    public void completeTask(Task task, ChildProfile child, ZoneId familyZone) {
        if (currentPeriodCompletion(task, child, familyZone).isPresent()) {
            throw new DomainException("TASK_ALREADY_COMPLETED", HttpStatus.CONFLICT,
                    "Aquesta tasca ja s'ha marcat com a feta per a aquest període");
        }

        TaskReward reward = requireActiveReward(task);
        taskCompletionRepository.save(TaskCompletion.builder()
                .task(task)
                .child(child)
                .completedAt(Instant.now())
                .status(TaskCompletionStatus.PENDING)
                .completionGroupId(UUID.randomUUID())
                .rewardMoney(reward.getMoneyAmount())
                .rewardScreenMinutes(reward.getScreenMinutes())
                .build());
    }

    /** Finalització col·laborativa d'una tasca Extra (Prompt 15): crea una fila per
     * participant (qui truca + col·laboradors), totes amb el mateix completion_group_id.
     * Bloqueja a l'instant la tasca per a la resta de la família fins que s'aprovi o
     * es rebutgi el grup. */
    @Transactional
    public void completeCollaborative(Task task, ChildProfile initiator, List<ChildProfile> collaborators, ZoneId familyZone) {
        if (currentPeriodGroup(task, familyZone).isPresent()) {
            throw new DomainException("TASK_ALREADY_CLAIMED", HttpStatus.CONFLICT,
                    "Aquesta tasca ja ha estat reclamada per a aquest període");
        }

        TaskReward reward = requireActiveReward(task);
        UUID groupId = UUID.randomUUID();
        List<ChildProfile> participants = new ArrayList<>();
        participants.add(initiator);
        for (ChildProfile collaborator : collaborators) {
            if (participants.stream().noneMatch(p -> p.getId().equals(collaborator.getId()))) {
                participants.add(collaborator);
            }
        }

        for (ChildProfile participant : participants) {
            taskCompletionRepository.save(TaskCompletion.builder()
                    .task(task)
                    .child(participant)
                    .completedAt(Instant.now())
                    .status(TaskCompletionStatus.PENDING)
                    .completionGroupId(groupId)
                    .rewardMoney(reward.getMoneyAmount())
                    .rewardScreenMinutes(reward.getScreenMinutes())
                    .build());
        }
    }

    private TaskReward requireActiveReward(Task task) {
        return taskRewardRepository.findByTaskIdAndActiveTrue(task.getId())
                .orElseThrow(() -> new DomainException("INVALID_ALLOWANCE_RULE", HttpStatus.CONFLICT,
                        "Aquesta tasca no té cap recompensa activa configurada"));
    }

    private ChildTaskResponse toResponse(Task task, ChildProfile child, ZoneId familyZone) {
        TaskReward reward = taskRewardRepository.findByTaskIdAndActiveTrue(task.getId()).orElse(null);
        java.math.BigDecimal rewardMoney = reward != null ? reward.getMoneyAmount() : java.math.BigDecimal.ZERO;
        int rewardMinutes = reward != null ? reward.getScreenMinutes() : 0;

        if (task.getTaskType() == TaskType.EXTRA) {
            Optional<List<TaskCompletion>> group = currentPeriodGroup(task, familyZone);
            if (group.isEmpty()) {
                return new ChildTaskResponse(task.getId(), task.getName(), task.getDescription(), task.getIcon(),
                        task.getTaskType(), rewardMoney, rewardMinutes, ChildTaskStatus.AVAILABLE, List.of());
            }
            List<TaskCompletion> completions = group.get();
            List<String> names = completions.stream().map(c -> c.getChild().getDisplayName()).toList();
            Optional<TaskCompletion> own = completions.stream().filter(c -> c.getChild().getId().equals(child.getId())).findFirst();
            ChildTaskStatus status = own.map(c -> mapStatus(c.getStatus())).orElse(ChildTaskStatus.CLAIMED_BY_OTHERS);
            return new ChildTaskResponse(task.getId(), task.getName(), task.getDescription(), task.getIcon(),
                    task.getTaskType(), rewardMoney, rewardMinutes, status, names);
        }

        ChildTaskStatus status = currentPeriodCompletion(task, child, familyZone)
                .map(c -> mapStatus(c.getStatus()))
                .orElse(ChildTaskStatus.AVAILABLE);
        return new ChildTaskResponse(task.getId(), task.getName(), task.getDescription(), task.getIcon(),
                task.getTaskType(), rewardMoney, rewardMinutes, status, List.of());
    }

    private ChildTaskStatus mapStatus(TaskCompletionStatus status) {
        return switch (status) {
            case PENDING -> ChildTaskStatus.PENDING;
            case APPROVED -> ChildTaskStatus.APPROVED;
            case REJECTED -> ChildTaskStatus.REJECTED;
            case CANCELLED -> ChildTaskStatus.AVAILABLE;
        };
    }

    private Optional<TaskCompletion> currentPeriodCompletion(Task task, ChildProfile child, ZoneId familyZone) {
        return taskCompletionRepository.findByTaskIdAndChildIdOrderByCompletedAtDesc(task.getId(), child.getId())
                .stream()
                .filter(c -> c.getStatus() != TaskCompletionStatus.CANCELLED)
                .filter(c -> RecurrenceWindow.isWithinCurrentPeriod(c.getCompletedAt(), task.getRecurrenceType(), familyZone))
                .findFirst();
    }

    /** Grup de finalització actiu per a la tasca (qualsevol fill), dins del període vigent —
     * és el que bloqueja una tasca Extra per a tota la família un cop reclamada. */
    private Optional<List<TaskCompletion>> currentPeriodGroup(Task task, ZoneId familyZone) {
        List<TaskCompletion> all = taskCompletionRepository.findByTaskId(task.getId());
        Optional<TaskCompletion> anchor = all.stream()
                .filter(c -> c.getStatus() != TaskCompletionStatus.CANCELLED)
                .filter(c -> RecurrenceWindow.isWithinCurrentPeriod(c.getCompletedAt(), task.getRecurrenceType(), familyZone))
                .findFirst();
        return anchor.map(a -> all.stream()
                .filter(c -> Objects.equals(c.getCompletionGroupId(), a.getCompletionGroupId()))
                .toList());
    }
}
