package cat.mapaka.family;

import cat.mapaka.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class FamilySettingsController {

    private final FamilyAccessService familyAccessService;
    private final FamilyRepository familyRepository;

    public FamilySettingsController(FamilyAccessService familyAccessService, FamilyRepository familyRepository) {
        this.familyAccessService = familyAccessService;
        this.familyRepository = familyRepository;
    }

    @GetMapping("/api/families/{id}/settings")
    public FamilySettingsResponse settings(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        Family family = familyAccessService.requireParentAccess(id, user);
        return FamilySettingsResponse.from(family);
    }

    @PreAuthorize("hasRole('PARENT')")
    @PatchMapping("/api/families/{id}/settings")
    public FamilySettingsResponse updateSettings(
            @PathVariable UUID id,
            @Valid @RequestBody FamilySettingsUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        Family family = familyAccessService.requireParentAccess(id, user);
        family.setTaskApprovalRequired(request.taskApprovalRequired());
        family.setNotifyPendingApprovalsEnabled(request.notifyPendingApprovalsEnabled());
        family.setAllowSavingsTransfer(request.allowSavingsTransfer());
        familyRepository.save(family);
        return FamilySettingsResponse.from(family);
    }
}
