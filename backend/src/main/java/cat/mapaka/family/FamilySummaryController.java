package cat.mapaka.family;

import cat.mapaka.child.ChildProfile;
import cat.mapaka.child.ChildProfileRepository;
import cat.mapaka.money.FamilyMoneyTransactionResponse;
import cat.mapaka.money.MoneyTransactionRepository;
import cat.mapaka.money.WalletType;
import cat.mapaka.security.AuthenticatedUser;
import cat.mapaka.task.TaskCompletionRepository;
import cat.mapaka.task.TaskCompletionStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class FamilySummaryController {

    private final FamilyAccessService familyAccessService;
    private final ChildProfileRepository childProfileRepository;
    private final MoneyTransactionRepository moneyTransactionRepository;
    private final TaskCompletionRepository taskCompletionRepository;

    public FamilySummaryController(
            FamilyAccessService familyAccessService,
            ChildProfileRepository childProfileRepository,
            MoneyTransactionRepository moneyTransactionRepository,
            TaskCompletionRepository taskCompletionRepository) {
        this.familyAccessService = familyAccessService;
        this.childProfileRepository = childProfileRepository;
        this.moneyTransactionRepository = moneyTransactionRepository;
        this.taskCompletionRepository = taskCompletionRepository;
    }

    @GetMapping("/api/families/{id}/summary")
    public List<ChildFamilySummary> summary(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        familyAccessService.requireParentAccess(id, user);
        return childProfileRepository.findAllByFamilyId(id).stream()
                .map(this::toSummary)
                .toList();
    }

    @GetMapping("/api/families/{id}/money-transactions")
    public List<FamilyMoneyTransactionResponse> movements(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        familyAccessService.requireParentAccess(id, user);
        return moneyTransactionRepository.findByFamilyIdOrderByCreatedAtDesc(id).stream()
                .map(FamilyMoneyTransactionResponse::from)
                .toList();
    }

    private ChildFamilySummary toSummary(ChildProfile child) {
        var spending = moneyTransactionRepository.balanceFor(child.getId(), WalletType.SPENDING);
        var savings = moneyTransactionRepository.balanceFor(child.getId(), WalletType.SAVINGS);
        long pending = taskCompletionRepository.countByChildIdAndStatus(child.getId(), TaskCompletionStatus.PENDING);
        return new ChildFamilySummary(child.getId(), child.getDisplayName(), child.getAvatar(), spending, savings, pending);
    }
}
