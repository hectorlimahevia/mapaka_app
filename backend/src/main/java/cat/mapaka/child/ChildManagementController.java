package cat.mapaka.child;

import cat.mapaka.allowance.AllowanceRuleUpdateRequest;
import cat.mapaka.common.DomainException;
import cat.mapaka.family.Family;
import cat.mapaka.family.FamilyAccessService;
import cat.mapaka.screentime.ScreenTimeRuleUpdateRequest;
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
@PreAuthorize("hasRole('PARENT')")
public class ChildManagementController {

    private final FamilyAccessService familyAccessService;
    private final ChildProfileRepository childProfileRepository;
    private final ChildManagementService childManagementService;

    public ChildManagementController(
            FamilyAccessService familyAccessService,
            ChildProfileRepository childProfileRepository,
            ChildManagementService childManagementService) {
        this.familyAccessService = familyAccessService;
        this.childProfileRepository = childProfileRepository;
        this.childManagementService = childManagementService;
    }

    @PostMapping("/api/children")
    public ResponseEntity<ChildDetailResponse> create(
            @Valid @RequestBody CreateChildRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        Family family = familyAccessService.requireParentAccess(user.familyId(), user);
        return ResponseEntity.ok(childManagementService.createChild(family, request));
    }

    @GetMapping("/api/families/{id}/children/detail")
    public List<ChildDetailResponse> details(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        familyAccessService.requireParentAccess(id, user);
        return childManagementService.detailsFor(id);
    }

    @PatchMapping("/api/children/{childId}/allowance-rule")
    public void updateAllowance(
            @PathVariable UUID childId,
            @Valid @RequestBody AllowanceRuleUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        ChildProfile child = requireChildInFamily(childId, user);
        childManagementService.updateAllowanceRule(child, request);
    }

    @PatchMapping("/api/children/{childId}/screen-time-rule")
    public void updateScreenTime(
            @PathVariable UUID childId,
            @Valid @RequestBody ScreenTimeRuleUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        ChildProfile child = requireChildInFamily(childId, user);
        childManagementService.updateScreenTimeRule(child, request);
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
