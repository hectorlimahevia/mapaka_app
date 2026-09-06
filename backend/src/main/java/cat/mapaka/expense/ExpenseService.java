package cat.mapaka.expense;

import cat.mapaka.child.ChildProfile;
import cat.mapaka.common.DomainException;
import cat.mapaka.common.TransactionType;
import cat.mapaka.money.MoneySourceType;
import cat.mapaka.money.MoneyTransaction;
import cat.mapaka.money.MoneyTransactionRepository;
import cat.mapaka.money.WalletType;
import cat.mapaka.security.AuthenticatedUser;
import cat.mapaka.user.User;
import cat.mapaka.user.UserRepository;
import cat.mapaka.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final MoneyTransactionRepository moneyTransactionRepository;
    private final UserRepository userRepository;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            MoneyTransactionRepository moneyTransactionRepository,
            UserRepository userRepository) {
        this.expenseRepository = expenseRepository;
        this.moneyTransactionRepository = moneyTransactionRepository;
        this.userRepository = userRepository;
    }

    /** Un pare sempre pot reflectir un gasto d'un fill amb efecte immediat (mateix criteri
     * que AdjustmentService); el propi fill només si `canLogExpenses` ho permet, i llavors
     * queda PENDING fins que un pare l'aprova (mateix patró que TaskCompletion). */
    @Transactional
    public ExpenseResponse create(ChildProfile child, AuthenticatedUser requester, CreateExpenseRequest request) {
        boolean isParent = requester.role() == UserRole.PARENT;
        if (!isParent && !child.isCanLogExpenses()) {
            throw new DomainException("CHILD_EXPENSES_DISABLED", HttpStatus.FORBIDDEN,
                    "Aquest fill no pot registrar els seus propis gastos");
        }
        User actor = userRepository.getReferenceById(requester.userId());

        if (isParent) {
            Expense expense = expenseRepository.save(Expense.builder()
                    .child(child).amount(request.amount()).reason(request.reason())
                    .status(ExpenseStatus.APPROVED).createdBy(actor)
                    .reviewedBy(actor).reviewedAt(Instant.now())
                    .build());
            writeLedger(expense, actor);
            return ExpenseResponse.from(expense);
        }

        Expense expense = expenseRepository.save(Expense.builder()
                .child(child).amount(request.amount()).reason(request.reason())
                .status(ExpenseStatus.PENDING).createdBy(actor)
                .build());
        return ExpenseResponse.from(expense);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> pending(UUID familyId) {
        return expenseRepository.findByFamilyIdAndStatus(familyId, ExpenseStatus.PENDING).stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> pendingForChild(UUID childId) {
        return expenseRepository.findByChildIdAndStatusOrderByCreatedAtDesc(childId, ExpenseStatus.PENDING).stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    @Transactional
    public ExpenseResponse approve(Expense expense, UUID approvingUserId) {
        requirePending(expense);
        User parent = userRepository.getReferenceById(approvingUserId);
        expense.setStatus(ExpenseStatus.APPROVED);
        expense.setReviewedBy(parent);
        expense.setReviewedAt(Instant.now());
        expenseRepository.save(expense);
        writeLedger(expense, parent);
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public void reject(Expense expense, UUID rejectingUserId) {
        requirePending(expense);
        expense.setStatus(ExpenseStatus.REJECTED);
        expense.setReviewedBy(userRepository.getReferenceById(rejectingUserId));
        expense.setReviewedAt(Instant.now());
        expenseRepository.save(expense);
    }

    private void writeLedger(Expense expense, User actor) {
        moneyTransactionRepository.save(MoneyTransaction.builder()
                .child(expense.getChild())
                .walletType(WalletType.SPENDING)
                .transactionType(TransactionType.DEBIT)
                .amount(expense.getAmount())
                .description(expense.getReason())
                .sourceType(MoneySourceType.PURCHASE)
                .sourceId(expense.getId())
                .createdBy(actor)
                .build());
    }

    private void requirePending(Expense expense) {
        if (expense.getStatus() != ExpenseStatus.PENDING) {
            throw new DomainException("EXPENSE_ALREADY_RESOLVED", HttpStatus.CONFLICT,
                    "Aquest gasto ja s'ha revisat");
        }
    }
}
