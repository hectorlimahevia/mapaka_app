package cat.mapaka.savings;

import cat.mapaka.child.ChildAccessService;
import cat.mapaka.child.ChildProfile;
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
    private final SavingsGoalService savingsGoalService;

    public SavingsGoalController(ChildAccessService childAccessService, SavingsGoalService savingsGoalService) {
        this.childAccessService = childAccessService;
        this.savingsGoalService = savingsGoalService;
    }

    @GetMapping("/api/children/{childId}/savings-goals")
    public List<SavingsGoalResponse> goals(@PathVariable UUID childId, @AuthenticationPrincipal AuthenticatedUser user) {
        childAccessService.requireAccess(childId, user);
        return savingsGoalService.list(childId);
    }

    /** El propi fill pot crear-se un objectiu nou (Prompt 8) — no requereix aprovació,
     * a diferència de les recompenses: un objectiu és només una etiqueta de progrés. */
    @PostMapping("/api/children/{childId}/savings-goals")
    public ResponseEntity<SavingsGoalResponse> create(
            @PathVariable UUID childId,
            @Valid @RequestBody CreateSavingsGoalRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        ChildProfile child = childAccessService.requireAccess(childId, user);
        return ResponseEntity.ok(savingsGoalService.create(child, request));
    }

    @PatchMapping("/api/children/{childId}/savings-goals/{goalId}")
    public SavingsGoalResponse update(
            @PathVariable UUID childId,
            @PathVariable UUID goalId,
            @Valid @RequestBody CreateSavingsGoalRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        childAccessService.requireAccess(childId, user);
        SavingsGoal goal = savingsGoalService.requireOwnedBy(goalId, childId);
        return savingsGoalService.update(goal, request);
    }

    /** El propi fill mou un import puntual del seu "per gastar" cap a un objectiu (ajust
     * posterior) — mai pot superar el que té disponible en aquell moment. */
    @PostMapping("/api/children/{childId}/savings-goals/{goalId}/contributions")
    public SavingsGoalResponse contribute(
            @PathVariable UUID childId,
            @PathVariable UUID goalId,
            @Valid @RequestBody ContributeToGoalRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return savingsGoalService.contribute(childId, goalId, request.amount(), user);
    }

    /** Eliminar un objectiu, actiu o ja assolit (ajust posterior) — mai esborra el diner
     * que ja tingués guardat, només el retorna a "per gastar" (SavingsGoalService.delete). */
    @DeleteMapping("/api/children/{childId}/savings-goals/{goalId}")
    public void delete(
            @PathVariable UUID childId,
            @PathVariable UUID goalId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        savingsGoalService.delete(childId, goalId, user);
    }
}
