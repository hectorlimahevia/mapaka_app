package cat.mapaka.task;

import cat.mapaka.common.DomainException;
import cat.mapaka.common.TransactionType;
import cat.mapaka.money.MoneySourceType;
import cat.mapaka.money.MoneyTransaction;
import cat.mapaka.money.MoneyTransactionRepository;
import cat.mapaka.money.WalletType;
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
import java.util.UUID;

/**
 * Aprovar/rebutjar tasques (Família+.pdf secció 13.2 i RN-01/02/03/12): l'aprovació és
 * l'únic punt on una recompensa de tasca es converteix en moviments reals al ledger —
 * tot dins d'una única transacció.
 */
@Service
public class ApprovalService {

    private final TaskCompletionRepository taskCompletionRepository;
    private final MoneyTransactionRepository moneyTransactionRepository;
    private final ScreenTimeTransactionRepository screenTimeTransactionRepository;
    private final UserRepository userRepository;

    public ApprovalService(
            TaskCompletionRepository taskCompletionRepository,
            MoneyTransactionRepository moneyTransactionRepository,
            ScreenTimeTransactionRepository screenTimeTransactionRepository,
            UserRepository userRepository) {
        this.taskCompletionRepository = taskCompletionRepository;
        this.moneyTransactionRepository = moneyTransactionRepository;
        this.screenTimeTransactionRepository = screenTimeTransactionRepository;
        this.userRepository = userRepository;
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

        if (completion.getRewardMoney().compareTo(BigDecimal.ZERO) > 0) {
            moneyTransactionRepository.save(MoneyTransaction.builder()
                    .child(child).walletType(WalletType.SPENDING).transactionType(TransactionType.CREDIT)
                    .amount(completion.getRewardMoney()).description(completion.getTask().getName())
                    .sourceType(MoneySourceType.TASK).sourceId(completion.getId())
                    .createdBy(parent).build());
        }
        if (completion.getRewardSavings().compareTo(BigDecimal.ZERO) > 0) {
            moneyTransactionRepository.save(MoneyTransaction.builder()
                    .child(child).walletType(WalletType.SAVINGS).transactionType(TransactionType.CREDIT)
                    .amount(completion.getRewardSavings()).description(completion.getTask().getName())
                    .sourceType(MoneySourceType.TASK).sourceId(completion.getId())
                    .createdBy(parent).build());
        }
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

    private void requirePending(TaskCompletion completion) {
        if (completion.getStatus() != TaskCompletionStatus.PENDING) {
            throw new DomainException("TASK_ALREADY_APPROVED", HttpStatus.CONFLICT,
                    "Aquesta tasca ja s'ha revisat (" + completion.getStatus() + ")");
        }
    }
}
