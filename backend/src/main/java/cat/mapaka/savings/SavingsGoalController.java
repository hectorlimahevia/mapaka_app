package cat.mapaka.savings;

import cat.mapaka.child.ChildAccessService;
import cat.mapaka.child.ChildProfile;
import cat.mapaka.money.MoneyTransactionRepository;
import cat.mapaka.money.WalletType;
import cat.mapaka.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    /** El propi fill pot crear-se un objectiu nou (Prompt 8) — no requereix aprovació,
     * a diferència de les recompenses: un objectiu és només una etiqueta de progrés. */
    @PostMapping("/api/children/{childId}/savings-goals")
    public ResponseEntity<SavingsGoalResponse> create(
            @PathVariable UUID childId,
            @Valid @RequestBody CreateSavingsGoalRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        ChildProfile child = childAccessService.requireAccess(childId, user);
        SavingsGoal goal = savingsGoalRepository.save(SavingsGoal.builder()
                .child(child)
                .name(request.name())
                .targetAmount(request.targetAmount())
                .status(SavingsGoalStatus.ACTIVE)
                .build());
        var currentAmount = moneyTransactionRepository.balanceFor(childId, WalletType.SAVINGS);
        return ResponseEntity.ok(SavingsGoalResponse.from(goal, currentAmount));
    }
}
