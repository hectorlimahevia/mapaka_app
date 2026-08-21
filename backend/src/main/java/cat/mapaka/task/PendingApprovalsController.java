package cat.mapaka.task;

import cat.mapaka.family.FamilyAccessService;
import cat.mapaka.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class PendingApprovalsController {

    private final FamilyAccessService familyAccessService;
    private final TaskCompletionRepository taskCompletionRepository;

    public PendingApprovalsController(FamilyAccessService familyAccessService, TaskCompletionRepository taskCompletionRepository) {
        this.familyAccessService = familyAccessService;
        this.taskCompletionRepository = taskCompletionRepository;
    }

    @GetMapping("/api/families/{id}/pending-approvals")
    public List<PendingApprovalResponse> pendingApprovals(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        familyAccessService.requireParentAccess(id, user);
        return taskCompletionRepository.findByFamilyIdAndStatusOrderByCompletedAtAsc(id, TaskCompletionStatus.PENDING).stream()
                .map(PendingApprovalResponse::from)
                .toList();
    }
}
