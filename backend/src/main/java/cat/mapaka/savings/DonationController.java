package cat.mapaka.savings;

import cat.mapaka.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@PreAuthorize("hasRole('PARENT')")
public class DonationController {

    private final SavingsGoalService savingsGoalService;

    public DonationController(SavingsGoalService savingsGoalService) {
        this.savingsGoalService = savingsGoalService;
    }

    @PostMapping("/api/savings-goals/{goalId}/donations")
    public SavingsGoalResponse donate(
            @PathVariable UUID goalId,
            @Valid @RequestBody CreateDonationRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return savingsGoalService.donate(goalId, request, user);
    }
}
