package cat.mapaka.task;

import cat.mapaka.allowance.MoneySplitCalculator;
import cat.mapaka.child.ChildProfile;
import cat.mapaka.common.DomainException;
import cat.mapaka.common.TransactionType;
import cat.mapaka.money.MoneySourceType;
import cat.mapaka.screentime.ScreenSourceType;
import cat.mapaka.screentime.ScreenTimeTransaction;
import cat.mapaka.screentime.ScreenTimeTransactionRepository;
import cat.mapaka.user.User;
import cat.mapaka.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Tasques de Responsabilitat incomplertes i la seva penalització manual (Prompt 15) —
 * mai s'aplica sola: només quan el PARENT ho decideix explícitament des d'aquesta llista. */
@Service
public class TaskPenaltyService {

    private final TaskRepository taskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskCompletionRepository taskCompletionRepository;
    private final MoneySplitCalculator moneySplitCalculator;
    private final ScreenTimeTransactionRepository screenTimeTransactionRepository;
    private final UserRepository userRepository;

    public TaskPenaltyService(
            TaskRepository taskRepository,
            TaskAssignmentRepository taskAssignmentRepository,
            TaskCompletionRepository taskCompletionRepository,
            MoneySplitCalculator moneySplitCalculator,
            ScreenTimeTransactionRepository screenTimeTransactionRepository,
            UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.taskCompletionRepository = taskCompletionRepository;
        this.moneySplitCalculator = moneySplitCalculator;
        this.screenTimeTransactionRepository = screenTimeTransactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<IncompleteTaskResponse> incomplete(UUID familyId, Instant asOf) {
        List<IncompleteTaskResponse> result = new ArrayList<>();
        List<Task> responsibilities = taskRepository.findByFamilyId(familyId).stream()
                .filter(Task::isActive)
                .filter(t -> t.getTaskType() == TaskType.RESPONSIBILITY)
                .filter(t -> t.getRecurrenceType() != RecurrenceType.NONE)
                .toList();

        for (Task task : responsibilities) {
            for (TaskAssignment assignment : taskAssignmentRepository.findByTaskId(task.getId())) {
                if (!assignment.isActive()) {
                    continue;
                }
                ChildProfile child = assignment.getChild();
                ZoneId familyZone = ZoneId.of(child.getUser().getFamily().getTimezone());
                boolean approvedThisPeriod = taskCompletionRepository
                        .findByTaskIdAndChildIdOrderByCompletedAtDesc(task.getId(), child.getId()).stream()
                        .anyMatch(c -> c.getStatus() == TaskCompletionStatus.APPROVED
                                && RecurrenceWindow.isWithinCurrentPeriod(c.getCompletedAt(), task.getRecurrenceType(), familyZone, asOf));
                if (!approvedThisPeriod) {
                    result.add(new IncompleteTaskResponse(
                            task.getId(), task.getName(), child.getId(), child.getDisplayName(),
                            task.getPenaltyMoneyAmount(), task.getPenaltyScreenMinutes()));
                }
            }
        }
        return result;
    }

    @Transactional
    public void applyPenalty(Task task, ChildProfile child, UUID actingUserId) {
        if (task.getTaskType() != TaskType.RESPONSIBILITY) {
            throw new DomainException("INVALID_TASK_TYPE", HttpStatus.BAD_REQUEST,
                    "Només les tasques de Responsabilitat admeten penalització");
        }
        boolean hasPenalty = task.getPenaltyMoneyAmount().compareTo(BigDecimal.ZERO) > 0 || task.getPenaltyScreenMinutes() > 0;
        if (!hasPenalty) {
            throw new DomainException("INVALID_TASK_PENALTY", HttpStatus.BAD_REQUEST,
                    "Aquesta tasca no té cap penalització configurada");
        }
        User parent = userRepository.getReferenceById(actingUserId);

        if (task.getPenaltyMoneyAmount().compareTo(BigDecimal.ZERO) > 0) {
            moneySplitCalculator.apply(child, task.getPenaltyMoneyAmount(), TransactionType.DEBIT,
                    MoneySourceType.TASK_PENALTY, task.getId(), task.getName(), parent);
        }
        if (task.getPenaltyScreenMinutes() > 0) {
            ZoneId familyZone = ZoneId.of(child.getUser().getFamily().getTimezone());
            screenTimeTransactionRepository.save(ScreenTimeTransaction.builder()
                    .child(child).transactionType(TransactionType.DEBIT).minutes(task.getPenaltyScreenMinutes())
                    .description(task.getName()).sourceType(ScreenSourceType.PENALTY)
                    .sourceId(task.getId()).occurredOn(LocalDate.now(familyZone))
                    .createdBy(parent).build());
        }
    }
}
