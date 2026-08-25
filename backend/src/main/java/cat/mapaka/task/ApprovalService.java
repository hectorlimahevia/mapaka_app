package cat.mapaka.task;

import cat.mapaka.allowance.MoneySplitCalculator;
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
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Aprovar/rebutjar tasques (Família+.pdf secció 13.2 i RN-01/02/03/12): l'aprovació és
 * l'únic punt on una recompensa de tasca es converteix en moviments reals al ledger —
 * tot dins d'una única transacció.
 */
@Service
public class ApprovalService {

    private final TaskCompletionRepository taskCompletionRepository;
    private final ScreenTimeTransactionRepository screenTimeTransactionRepository;
    private final UserRepository userRepository;
    private final MoneySplitCalculator moneySplitCalculator;

    public ApprovalService(
            TaskCompletionRepository taskCompletionRepository,
            ScreenTimeTransactionRepository screenTimeTransactionRepository,
            UserRepository userRepository,
            MoneySplitCalculator moneySplitCalculator) {
        this.taskCompletionRepository = taskCompletionRepository;
        this.screenTimeTransactionRepository = screenTimeTransactionRepository;
        this.userRepository = userRepository;
        this.moneySplitCalculator = moneySplitCalculator;
    }

    @Transactional
    public void approve(TaskCompletion completion, UUID approvingUserId) {
        requirePending(completion);
        User parent = userRepository.getReferenceById(approvingUserId);

        completion.setStatus(TaskCompletionStatus.APPROVED);
        completion.setReviewedBy(parent);
        completion.setReviewedAt(Instant.now());
        taskCompletionRepository.save(completion);

        var child = completion.getChild();

        moneySplitCalculator.apply(child, completion.getRewardMoney(), TransactionType.CREDIT,
                MoneySourceType.TASK, completion.getId(), completion.getTask().getName(), parent);
        if (completion.getRewardScreenMinutes() > 0) {
            ZoneId familyZone = ZoneId.of(child.getUser().getFamily().getTimezone());
            screenTimeTransactionRepository.save(ScreenTimeTransaction.builder()
                    .child(child).transactionType(TransactionType.CREDIT).minutes(completion.getRewardScreenMinutes())
                    .description(completion.getTask().getName()).sourceType(ScreenSourceType.TASK)
                    .sourceId(completion.getId()).occurredOn(LocalDate.now(familyZone))
                    .createdBy(parent).build());
        }
    }

    @Transactional
    public void reject(TaskCompletion completion, UUID rejectingUserId) {
        requirePending(completion);
        completion.setStatus(TaskCompletionStatus.REJECTED);
        completion.setReviewedBy(userRepository.getReferenceById(rejectingUserId));
        completion.setReviewedAt(Instant.now());
        taskCompletionRepository.save(completion);
    }

    /** Aprova totes les files d'una finalització col·laborativa d'una tasca Extra (Prompt 15)
     * en un sol acte: reparteix a parts iguals entre els participants la recompensa que ja
     * es va desar a cada fila en completar-se (el mateix import total a totes), i després
     * passa la part de cadascú pel seu propi repartiment gastar/estalvi/objectius. */
    @Transactional
    public void approveGroup(List<TaskCompletion> group, UUID approvingUserId) {
        if (group.isEmpty()) {
            return;
        }
        for (TaskCompletion completion : group) {
            requirePending(completion);
        }
        User parent = userRepository.getReferenceById(approvingUserId);
        Task task = group.get(0).getTask();
        int participantCount = group.size();
        BigDecimal totalMoney = group.get(0).getRewardMoney();
        int totalMinutes = group.get(0).getRewardScreenMinutes();
        BigDecimal perParticipantMoney = totalMoney.divide(BigDecimal.valueOf(participantCount), 2, RoundingMode.HALF_UP);
        int perParticipantMinutes = totalMinutes / participantCount;

        for (TaskCompletion completion : group) {
            completion.setStatus(TaskCompletionStatus.APPROVED);
            completion.setReviewedBy(parent);
            completion.setReviewedAt(Instant.now());
            taskCompletionRepository.save(completion);

            var child = completion.getChild();
            moneySplitCalculator.apply(child, perParticipantMoney, TransactionType.CREDIT,
                    MoneySourceType.TASK, completion.getId(), task.getName(), parent);
            if (perParticipantMinutes > 0) {
                ZoneId familyZone = ZoneId.of(child.getUser().getFamily().getTimezone());
                screenTimeTransactionRepository.save(ScreenTimeTransaction.builder()
                        .child(child).transactionType(TransactionType.CREDIT).minutes(perParticipantMinutes)
                        .description(task.getName()).sourceType(ScreenSourceType.TASK)
                        .sourceId(completion.getId()).occurredOn(LocalDate.now(familyZone))
                        .createdBy(parent).build());
            }
        }
    }

    @Transactional
    public void rejectGroup(List<TaskCompletion> group, UUID rejectingUserId) {
        User parent = userRepository.getReferenceById(rejectingUserId);
        for (TaskCompletion completion : group) {
            requirePending(completion);
            completion.setStatus(TaskCompletionStatus.REJECTED);
            completion.setReviewedBy(parent);
            completion.setReviewedAt(Instant.now());
            taskCompletionRepository.save(completion);
        }
    }

    private void requirePending(TaskCompletion completion) {
        if (completion.getStatus() != TaskCompletionStatus.PENDING) {
            throw new DomainException("TASK_ALREADY_APPROVED", HttpStatus.CONFLICT,
                    "Aquesta tasca ja s'ha revisat (" + completion.getStatus() + ")");
        }
    }
}
