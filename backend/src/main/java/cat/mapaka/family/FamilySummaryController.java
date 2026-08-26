package cat.mapaka.family;

import cat.mapaka.child.ChildProfile;
import cat.mapaka.child.ChildProfileRepository;
import cat.mapaka.money.FamilyMoneyTransactionResponse;
import cat.mapaka.money.MoneyTransactionRepository;
import cat.mapaka.money.WalletType;
import cat.mapaka.savings.SavingsGoal;
import cat.mapaka.savings.SavingsGoalRepository;
import cat.mapaka.security.AuthenticatedUser;
import cat.mapaka.task.TaskCompletionRepository;
import cat.mapaka.task.TaskCompletionStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
public class FamilySummaryController {

    /** Substitueix `to` quan el filtre de període no s'aplica — evita passar un `Instant`
     * nul a la consulta filtrada (ambigüitat de tipus amb Postgres, vegeu el comentari
     * de `findByFamilyIdFiltered`). */
    private static final Instant FAR_FUTURE = Instant.parse("9999-12-31T23:59:59Z");

    private final FamilyAccessService familyAccessService;
    private final ChildProfileRepository childProfileRepository;
    private final MoneyTransactionRepository moneyTransactionRepository;
    private final TaskCompletionRepository taskCompletionRepository;
    private final SavingsGoalRepository savingsGoalRepository;

    public FamilySummaryController(
            FamilyAccessService familyAccessService,
            ChildProfileRepository childProfileRepository,
            MoneyTransactionRepository moneyTransactionRepository,
            TaskCompletionRepository taskCompletionRepository,
            SavingsGoalRepository savingsGoalRepository) {
        this.familyAccessService = familyAccessService;
        this.childProfileRepository = childProfileRepository;
        this.moneyTransactionRepository = moneyTransactionRepository;
        this.taskCompletionRepository = taskCompletionRepository;
        this.savingsGoalRepository = savingsGoalRepository;
    }

    @GetMapping("/api/families/{id}/summary")
    public List<ChildFamilySummary> summary(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        familyAccessService.requireParentAccess(id, user);
        return childProfileRepository.findAllByFamilyId(id).stream()
                .map(this::toSummary)
                .toList();
    }

    /** `from`/`to`/`childId` filtren el període i el fill (tots opcionals, Prompt 15
     * secció 7.2); `page`/`size` permeten l'accés paginat a l'historial complet des de
     * la pantalla "Veure tots els moviments" sense necessitat d'un segon endpoint. */
    @GetMapping("/api/families/{id}/money-transactions")
    public List<FamilyMoneyTransactionResponse> movements(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID childId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal AuthenticatedUser user) {
        familyAccessService.requireParentAccess(id, user);
        Instant effectiveFrom = from != null ? from : Instant.EPOCH;
        Instant effectiveTo = to != null ? to : FAR_FUTURE;
        return moneyTransactionRepository
                .findByFamilyIdFiltered(id, childId, effectiveFrom, effectiveTo, PageRequest.of(page, size))
                .stream()
                .map(FamilyMoneyTransactionResponse::from)
                .toList();
    }

    private ChildFamilySummary toSummary(ChildProfile child) {
        var spending = moneyTransactionRepository.balanceFor(child.getId(), WalletType.SPENDING);
        var savings = moneyTransactionRepository.balanceFor(child.getId(), WalletType.SAVINGS);
        long pending = taskCompletionRepository.countByChildIdAndStatus(child.getId(), TaskCompletionStatus.PENDING);

        // Tots els objectius, no només els ACTIVE: un cop COMPLETED l'import continua sent
        // diner real del fill (no torna mai a gastar/estalvi) — excloure'l del "Total" el faria
        // desaparèixer de la targeta en completar-se un objectiu, cosa que no té sentit.
        List<SavingsGoal> allGoals = savingsGoalRepository.findByChildId(child.getId());
        BigDecimal goalsTotal = BigDecimal.ZERO;
        List<GoalAllocationSummary> goals = new java.util.ArrayList<>();
        for (SavingsGoal goal : allGoals) {
            BigDecimal current = moneyTransactionRepository.goalProgress(goal.getId());
            goalsTotal = goalsTotal.add(current);
            goals.add(new GoalAllocationSummary(goal.getId(), goal.getName(), goal.getAllocationPercentage(), current));
        }
        BigDecimal total = spending.add(savings).add(goalsTotal);

        return new ChildFamilySummary(
                child.getId(), child.getDisplayName(), child.getAvatar(), child.getColorTheme(), child.getAvatarIcon(),
                spending, savings, total, pending, goals);
    }
}
