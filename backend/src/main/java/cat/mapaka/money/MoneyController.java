package cat.mapaka.money;

import cat.mapaka.allowance.AllowanceRuleService;
import cat.mapaka.child.ChildAccessService;
import cat.mapaka.child.ChildProfile;
import cat.mapaka.savings.SavingsGoal;
import cat.mapaka.savings.SavingsGoalRepository;
import cat.mapaka.savings.SavingsGoalStatus;
import cat.mapaka.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
public class MoneyController {

    private final ChildAccessService childAccessService;
    private final MoneyTransactionRepository moneyTransactionRepository;
    private final AllowanceRuleService allowanceRuleService;
    private final SavingsGoalRepository savingsGoalRepository;

    public MoneyController(
            ChildAccessService childAccessService,
            MoneyTransactionRepository moneyTransactionRepository,
            AllowanceRuleService allowanceRuleService,
            SavingsGoalRepository savingsGoalRepository) {
        this.childAccessService = childAccessService;
        this.moneyTransactionRepository = moneyTransactionRepository;
        this.allowanceRuleService = allowanceRuleService;
        this.savingsGoalRepository = savingsGoalRepository;
    }

    @GetMapping("/api/children/{id}/wallet")
    public WalletResponse wallet(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        ChildProfile child = childAccessService.requireAccess(id, user);
        var spending = moneyTransactionRepository.balanceFor(id, WalletType.SPENDING);
        var savings = moneyTransactionRepository.balanceFor(id, WalletType.SAVINGS);
        var spendingPercentage = allowanceRuleService.resolveSpendingPercentage(child);
        var allocatedGoalPercentage = savingsGoalRepository.findByChildIdAndStatus(id, SavingsGoalStatus.ACTIVE).stream()
                .map(SavingsGoal::getAllocationPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // El "total" inclou també el que hi ha guardat als objectius (actius o assolits, mai
        // els CANCELLED) — mateixa definició que ja feia servir FamilySummaryController pel
        // pare; sense això el fill veuria un "total" que no compta diner seu real (ajust
        // posterior, la card d'Inici encara no el mostrava).
        var goalsTotal = savingsGoalRepository.findByChildIdAndStatusNot(id, SavingsGoalStatus.CANCELLED).stream()
                .map(goal -> moneyTransactionRepository.goalProgress(goal.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var total = spending.add(savings).add(goalsTotal);
        return new WalletResponse(spending, savings, total, spendingPercentage, allocatedGoalPercentage);
    }

    @GetMapping("/api/children/{id}/money-transactions")
    public List<MoneyTransactionResponse> transactions(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        childAccessService.requireAccess(id, user);
        return moneyTransactionRepository.findByChildIdOrderByCreatedAtDesc(id).stream()
                .map(MoneyTransactionResponse::from)
                .toList();
    }
}
