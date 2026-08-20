package cat.mapaka.task;

import cat.mapaka.child.ChildProfile;
import cat.mapaka.common.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Tasques vistes pel fill (Família+.pdf secció 2.1 i 12-13). Marcar una tasca com a feta
 * només crea un TaskCompletion PENDING amb un snapshot de la recompensa vigent — mai toca
 * els ledgers directament (això és feina de l'aprovació del PARENT, Fase 7).
 */
@Service
public class TaskService {

    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskRewardRepository taskRewardRepository;
    private final TaskCompletionRepository taskCompletionRepository;

    public TaskService(
            TaskAssignmentRepository taskAssignmentRepository,
            TaskRewardRepository taskRewardRepository,
            TaskCompletionRepository taskCompletionRepository) {
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.taskRewardRepository = taskRewardRepository;
        this.taskCompletionRepository = taskCompletionRepository;
    }

    @Transactional(readOnly = true)
    public List<ChildTaskResponse> getTasksFor(ChildProfile child) {
        ZoneId familyZone = ZoneId.of(child.getUser().getFamily().getTimezone());

        return taskAssignmentRepository.findByChildId(child.getId()).stream()
                .filter(TaskAssignment::isActive)
                .map(TaskAssignment::getTask)
                .filter(Task::isActive)
                .map(task -> toResponse(task, child, familyZone))
                .toList();
    }

    @Transactional
    public void completeTask(Task task, ChildProfile child, ZoneId familyZone) {
        Optional<TaskCompletion> current = currentPeriodCompletion(task, child, familyZone);
        if (current.isPresent()) {
            throw new DomainException("TASK_ALREADY_COMPLETED", HttpStatus.CONFLICT,
                    "Aquesta tasca ja s'ha marcat com a feta per a aquest període");
        }

        TaskReward reward = taskRewardRepository.findByTaskIdAndActiveTrue(task.getId())
                .orElseThrow(() -> new DomainException("INVALID_ALLOWANCE_RULE", HttpStatus.CONFLICT,
                        "Aquesta tasca no té cap recompensa activa configurada"));

        taskCompletionRepository.save(TaskCompletion.builder()
                .task(task)
                .child(child)
                .completedAt(Instant.now())
                .status(TaskCompletionStatus.PENDING)
                .rewardMoney(reward.getMoneyAmount())
                .rewardSavings(reward.getSavingsAmount())
                .rewardScreenMinutes(reward.getScreenMinutes())
                .build());
    }

    private ChildTaskResponse toResponse(Task task, ChildProfile child, ZoneId familyZone) {
        TaskReward reward = taskRewardRepository.findByTaskIdAndActiveTrue(task.getId()).orElse(null);
        ChildTaskStatus status = currentPeriodCompletion(task, child, familyZone)
                .map(c -> switch (c.getStatus()) {
                    case PENDING -> ChildTaskStatus.PENDING;
                    case APPROVED -> ChildTaskStatus.APPROVED;
                    case REJECTED -> ChildTaskStatus.REJECTED;
                    case CANCELLED -> ChildTaskStatus.AVAILABLE;
                })
                .orElse(ChildTaskStatus.AVAILABLE);

        return new ChildTaskResponse(
                task.getId(),
                task.getName(),
                task.getDescription(),
                task.getIcon(),
                task.getTaskType(),
                reward != null ? reward.getMoneyAmount() : java.math.BigDecimal.ZERO,
                reward != null ? reward.getSavingsAmount() : java.math.BigDecimal.ZERO,
                reward != null ? reward.getScreenMinutes() : 0,
                status);
    }

    private Optional<TaskCompletion> currentPeriodCompletion(Task task, ChildProfile child, ZoneId familyZone) {
        return taskCompletionRepository.findByTaskIdAndChildIdOrderByCompletedAtDesc(task.getId(), child.getId())
                .stream()
                .filter(c -> c.getStatus() != TaskCompletionStatus.CANCELLED)
                .filter(c -> isWithinCurrentPeriod(c.getCompletedAt(), task.getRecurrenceType(), familyZone))
                .findFirst();
    }

    private boolean isWithinCurrentPeriod(Instant completedAt, RecurrenceType recurrenceType, ZoneId familyZone) {
        ZonedDateTime completed = completedAt.atZone(familyZone);
        ZonedDateTime now = ZonedDateTime.now(familyZone);
        WeekFields weekFields = WeekFields.of(Locale.forLanguageTag("ca"));

        return switch (recurrenceType) {
            case NONE -> true;
            case DAILY, CUSTOM -> completed.toLocalDate().equals(now.toLocalDate());
            case WEEKLY -> completed.get(weekFields.weekOfWeekBasedYear()) == now.get(weekFields.weekOfWeekBasedYear())
                    && completed.get(weekFields.weekBasedYear()) == now.get(weekFields.weekBasedYear());
            case MONTHLY -> completed.getYear() == now.getYear() && completed.getMonth() == now.getMonth();
        };
    }
}
