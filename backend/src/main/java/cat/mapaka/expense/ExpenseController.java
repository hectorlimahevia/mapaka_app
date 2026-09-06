package cat.mapaka.expense;

import cat.mapaka.child.ChildAccessService;
import cat.mapaka.child.ChildProfile;
import cat.mapaka.common.DomainException;
import cat.mapaka.family.FamilyAccessService;
import cat.mapaka.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ExpenseRepository expenseRepository;
    private final ChildAccessService childAccessService;
    private final FamilyAccessService familyAccessService;

    public ExpenseController(
            ExpenseService expenseService,
            ExpenseRepository expenseRepository,
            ChildAccessService childAccessService,
            FamilyAccessService familyAccessService) {
        this.expenseService = expenseService;
        this.expenseRepository = expenseRepository;
        this.childAccessService = childAccessService;
        this.familyAccessService = familyAccessService;
    }

    @PreAuthorize("hasAnyRole('PARENT','CHILD')")
    @PostMapping("/api/children/{childId}/expenses")
    public ResponseEntity<ExpenseResponse> create(
            @PathVariable UUID childId,
            @Valid @RequestBody CreateExpenseRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        ChildProfile child = childAccessService.requireAccess(childId, user);
        return ResponseEntity.ok(expenseService.create(child, user, request));
    }

    @PreAuthorize("hasAnyRole('PARENT','CHILD')")
    @GetMapping("/api/children/{childId}/expenses/pending")
    public List<ExpenseResponse> pendingForChild(@PathVariable UUID childId, @AuthenticationPrincipal AuthenticatedUser user) {
        childAccessService.requireAccess(childId, user);
        return expenseService.pendingForChild(childId);
    }

    @PreAuthorize("hasRole('PARENT')")
    @GetMapping("/api/families/{id}/pending-expenses")
    public List<ExpenseResponse> pending(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        familyAccessService.requireParentAccess(id, user);
        return expenseService.pending(id);
    }

    @PreAuthorize("hasRole('PARENT')")
    @PostMapping("/api/expenses/{id}/approve")
    public ExpenseResponse approve(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        Expense expense = fetchAndAuthorize(id, user);
        return expenseService.approve(expense, user.userId());
    }

    @PreAuthorize("hasRole('PARENT')")
    @PostMapping("/api/expenses/{id}/reject")
    public void reject(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        Expense expense = fetchAndAuthorize(id, user);
        expenseService.reject(expense, user.userId());
    }

    private Expense fetchAndAuthorize(UUID id, AuthenticatedUser user) {
        Expense expense = expenseRepository.findByIdFetchChildAndFamily(id)
                .orElseThrow(() -> new DomainException("EXPENSE_NOT_FOUND", HttpStatus.NOT_FOUND, "Gasto no trobat"));
        if (!expense.getChild().getUser().getFamily().getId().equals(user.familyId())) {
            throw new DomainException("ACCESS_DENIED", HttpStatus.FORBIDDEN, "No pots revisar gastos d'una altra família");
        }
        return expense;
    }
}
