package cat.mapaka.savings;

import cat.mapaka.child.ChildAccessService;
import cat.mapaka.money.MoneyTransactionRepository;
import cat.mapaka.money.WalletType;
import cat.mapaka.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class SavingsGoalController {

    private final ChildAccessService childAccessService;
    private final SavingsGoalRepository savingsGoalRepository;
    private final MoneyTransactionRepository moneyTransactionRepository;

    public SavingsGoalController(
            ChildAccessService childAccessService,
            SavingsGoalRepository savingsGoalRepository,
            MoneyTransactionRepository moneyTransactionRepository) {
        this.childAccessService = childAccessService;
        this.savingsGoalRepository = savingsGoalRepository;
        this.moneyTransactionRepository = moneyTransactionRepository;
    }

    @GetMapping("/api/children/{childId}/savings-goals")
    public List<SavingsGoalResponse> goals(@PathVariable UUID childId, @AuthenticationPrincipal AuthenticatedUser user) {
        childAccessService.requireAccess(childId, user);
        var currentAmount = moneyTransactionRepository.balanceFor(childId, WalletType.SAVINGS);
        return savingsGoalRepository.findByChildId(childId).stream()
                .map(goal -> SavingsGoalResponse.from(goal, currentAmount))
                .toList();
    }
}
