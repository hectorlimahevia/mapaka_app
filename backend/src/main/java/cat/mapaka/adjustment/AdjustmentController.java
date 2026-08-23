package cat.mapaka.adjustment;

import cat.mapaka.child.ChildProfile;
import cat.mapaka.child.ChildProfileRepository;
import cat.mapaka.common.DomainException;
import cat.mapaka.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@PreAuthorize("hasRole('PARENT')")
public class AdjustmentController {

    private final AdjustmentService adjustmentService;
    private final ChildProfileRepository childProfileRepository;

    public AdjustmentController(AdjustmentService adjustmentService, ChildProfileRepository childProfileRepository) {
        this.adjustmentService = adjustmentService;
        this.childProfileRepository = childProfileRepository;
    }

    @PostMapping("/api/children/{id}/money-adjustments")
    public void moneyAdjustment(
            @PathVariable UUID id, @Valid @RequestBody MoneyAdjustmentRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        ChildProfile child = requireChildInFamily(id, user);
        adjustmentService.applyMoney(child, request, user.userId());
    }

    @PostMapping("/api/children/{id}/screen-time/adjustments")
    public void screenTimeAdjustment(
            @PathVariable UUID id, @Valid @RequestBody ScreenTimeAdjustmentRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        ChildProfile child = requireChildInFamily(id, user);
        adjustmentService.applyScreenTime(child, request, user.userId());
    }

    private ChildProfile requireChildInFamily(UUID childId, AuthenticatedUser user) {
        ChildProfile child = childProfileRepository.findByIdFetchUserAndFamily(childId)
                .orElseThrow(() -> new DomainException("CHILD_NOT_FOUND", HttpStatus.NOT_FOUND, "Fill no trobat"));
        if (!child.getUser().getFamily().getId().equals(user.familyId())) {
            throw new DomainException("ACCESS_DENIED", HttpStatus.FORBIDDEN, "No pots gestionar un fill d'una altra família");
        }
        return child;
    }
}
