package cat.mapaka.task;

import cat.mapaka.child.ChildProfile;
import cat.mapaka.child.ChildProfileRepository;
import cat.mapaka.common.DomainException;
import cat.mapaka.family.Family;
import cat.mapaka.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Alta, edició i baixa de tasques per al PARENT (Prompt 10) — el forat real que ni la
 * maqueta ni el Prompt 9 originals cobrien: sense això no hi havia manera de donar d'alta
 * cap tasca des de la interfície.
 */
@Service
public class TaskManagementService {

    private final TaskRepository taskRepository;
    private final TaskRewardRepository taskRewardRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final ChildProfileRepository childProfileRepository;

    public TaskManagementService(
            TaskRepository taskRepository,
            TaskRewardRepository taskRewardRepository,
            TaskAssignmentRepository taskAssignmentRepository,
            ChildProfileRepository childProfileRepository) {
        this.taskRepository = taskRepository;
        this.taskRewardRepository = taskRewardRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.childProfileRepository = childProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskManagementResponse> list(UUID familyId, TaskType type, Boolean active, UUID childId) {
        return taskRepository.findByFamilyId(familyId).stream()
                .filter(t -> type == null || t.getTaskType() == type)
                .filter(t -> active == null || t.isActive() == active)
                .filter(t -> childId == null || taskAssignmentRepository.findByTaskId(t.getId()).stream()
                        .anyMatch(a -> a.isActive() && a.getChild().getId().equals(childId)))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TaskManagementResponse create(Family family, User createdBy, CreateTaskRequest request) {
        requireValidRewardOrPenalty(request.taskType(), request.rewardMoney(), request.rewardScreenMinutes(),
                request.penaltyMoneyAmount(), request.penaltyScreenMinutes());
        // Una tasca Extra no té assignació fixa — és visible per a tots els fills de la
        // família (Prompt 15); els childIds que arribin per a una Extra s'ignoren.
        Set<ChildProfile> children = request.taskType() == TaskType.EXTRA
                ? Set.of() : resolveChildren(family.getId(), request.childIds());

        Task task = taskRepository.save(Task.builder()
                .family(family)
                .name(request.name())
                .description(request.description())
                .taskType(request.taskType())
                .icon(request.icon())
                .active(true)
                .requiresApproval(request.requiresApproval())
                .repeatable(request.recurrenceType() != RecurrenceType.NONE)
                .recurrenceType(request.recurrenceType())
                .penaltyMoneyAmount(penaltyOrZero(request.penaltyMoneyAmount()))
                .penaltyScreenMinutes(request.penaltyScreenMinutes() != null ? request.penaltyScreenMinutes() : 0)
                .createdBy(createdBy)
                .build());

        taskRewardRepository.save(TaskReward.builder()
                .task(task)
                .moneyAmount(request.rewardMoney())
                .screenMinutes(request.rewardScreenMinutes())
                .active(true)
                .build());

        for (ChildProfile child : children) {
            taskAssignmentRepository.save(TaskAssignment.builder().task(task).child(child).active(true).build());
        }

        return toResponse(task);
    }

    @Transactional
    public TaskManagementResponse update(Task task, CreateTaskRequest request) {
        requireValidRewardOrPenalty(request.taskType(), request.rewardMoney(), request.rewardScreenMinutes(),
                request.penaltyMoneyAmount(), request.penaltyScreenMinutes());
        Set<ChildProfile> children = request.taskType() == TaskType.EXTRA
                ? Set.of() : resolveChildren(task.getFamily().getId(), request.childIds());

        task.setName(request.name());
        task.setDescription(request.description());
        task.setTaskType(request.taskType());
        task.setIcon(request.icon());
        task.setRequiresApproval(request.requiresApproval());
        task.setRepeatable(request.recurrenceType() != RecurrenceType.NONE);
        task.setRecurrenceType(request.recurrenceType());
        task.setPenaltyMoneyAmount(penaltyOrZero(request.penaltyMoneyAmount()));
        task.setPenaltyScreenMinutes(request.penaltyScreenMinutes() != null ? request.penaltyScreenMinutes() : 0);
        taskRepository.save(task);

        // Cada TaskCompletion ja guarda una còpia de la recompensa en completar-se
        // (ApprovalService), així que actualitzar-la aquí no altera res del passat.
        taskRewardRepository.findByTaskIdAndActiveTrue(task.getId()).ifPresentOrElse(
                reward -> {
                    reward.setMoneyAmount(request.rewardMoney());
                    reward.setScreenMinutes(request.rewardScreenMinutes());
                    taskRewardRepository.save(reward);
                },
                () -> taskRewardRepository.save(TaskReward.builder()
                        .task(task).moneyAmount(request.rewardMoney())
                        .screenMinutes(request.rewardScreenMinutes()).active(true).build()));

        if (task.getTaskType() == TaskType.EXTRA) {
            // Si una tasca passa a ser Extra en editar-la, es descarta qualsevol assignació prèvia.
            for (TaskAssignment assignment : taskAssignmentRepository.findByTaskId(task.getId())) {
                if (assignment.isActive()) {
                    assignment.setActive(false);
                    taskAssignmentRepository.save(assignment);
                }
            }
        } else {
            Set<UUID> keepChildIds = children.stream().map(ChildProfile::getId).collect(java.util.stream.Collectors.toSet());
            List<TaskAssignment> current = taskAssignmentRepository.findByTaskId(task.getId());
            for (TaskAssignment assignment : current) {
                boolean shouldStayActive = keepChildIds.contains(assignment.getChild().getId());
                if (assignment.isActive() != shouldStayActive) {
                    assignment.setActive(shouldStayActive);
                    taskAssignmentRepository.save(assignment);
                }
            }
            Set<UUID> alreadyAssigned = current.stream().map(a -> a.getChild().getId()).collect(java.util.stream.Collectors.toSet());
            for (ChildProfile child : children) {
                if (!alreadyAssigned.contains(child.getId())) {
                    taskAssignmentRepository.save(TaskAssignment.builder().task(task).child(child).active(true).build());
                }
            }
        }

        return toResponse(task);
    }

    private BigDecimal penaltyOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /** Mai un DELETE físic si la tasca ja té completions associades — es dona de baixa
     * (active=false), mateix criteri que ja s'aplica als fills. */
    @Transactional
    public void deactivate(Task task) {
        task.setActive(false);
        taskRepository.save(task);
    }

    private Set<ChildProfile> resolveChildren(UUID familyId, List<UUID> childIds) {
        Set<ChildProfile> children = new HashSet<>();
        for (UUID childId : childIds) {
            ChildProfile child = childProfileRepository.findByIdFetchUserAndFamily(childId)
                    .orElseThrow(() -> new DomainException("CHILD_NOT_FOUND", HttpStatus.NOT_FOUND, "Fill no trobat"));
            if (!child.getUser().getFamily().getId().equals(familyId)) {
                throw new DomainException("ACCESS_DENIED", HttpStatus.FORBIDDEN, "No pots assignar un fill d'una altra família");
            }
            children.add(child);
        }
        return children;
    }

    /** Una tasca Extra sempre necessita recompensa (és l'únic incentiu per fer-la). Una
     * Responsabilitat, en canvi, sempre necessita penalització — la recompensa hi és
     * opcional, però mai una Responsabilitat amb recompensa i sense penalització: sense
     * penalització no hi ha conseqüència real de no fer-la (feedback explícit: "con
     * recompensa y penalizacion" / "sin recompensa y penalizacion" són vàlides, "con
     * recompensa y sin penalizacion" no ho és). */
    private void requireValidRewardOrPenalty(
            TaskType taskType, BigDecimal rewardMoney, int rewardScreenMinutes,
            BigDecimal penaltyMoneyAmount, Integer penaltyScreenMinutes) {
        if (taskType == TaskType.EXTRA) {
            boolean hasReward = rewardMoney.compareTo(BigDecimal.ZERO) > 0 || rewardScreenMinutes > 0;
            if (!hasReward) {
                throw new DomainException("INVALID_TASK_REWARD", HttpStatus.BAD_REQUEST,
                        "La tasca ha de tenir com a mínim una recompensa (diners o minuts)");
            }
            return;
        }
        BigDecimal penalty = penaltyOrZero(penaltyMoneyAmount);
        int penaltyMinutes = penaltyScreenMinutes != null ? penaltyScreenMinutes : 0;
        boolean hasPenalty = penalty.compareTo(BigDecimal.ZERO) > 0 || penaltyMinutes > 0;
        if (!hasPenalty) {
            throw new DomainException("INVALID_TASK_PENALTY", HttpStatus.BAD_REQUEST,
                    "Aquesta tasca no té cap penalització configurada");
        }
    }

    private TaskManagementResponse toResponse(Task task) {
        TaskReward reward = taskRewardRepository.findByTaskIdAndActiveTrue(task.getId()).orElse(null);
        List<TaskManagementResponse.AssignedChild> assigned = taskAssignmentRepository.findByTaskId(task.getId()).stream()
                .filter(TaskAssignment::isActive)
                .map(a -> new TaskManagementResponse.AssignedChild(a.getChild().getId(), a.getChild().getDisplayName()))
                .toList();
        return new TaskManagementResponse(
                task.getId(),
                task.getName(),
                task.getDescription(),
                task.getTaskType(),
                task.getIcon(),
                task.isRequiresApproval(),
                task.isActive(),
                task.getRecurrenceType(),
                reward != null ? reward.getMoneyAmount() : BigDecimal.ZERO,
                reward != null ? reward.getScreenMinutes() : 0,
                task.getPenaltyMoneyAmount(),
                task.getPenaltyScreenMinutes(),
                assigned);
    }
}
