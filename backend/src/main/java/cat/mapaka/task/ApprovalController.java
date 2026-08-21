package cat.mapaka.task;

import cat.mapaka.common.DomainException;
import cat.mapaka.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@PreAuthorize("hasRole('PARENT')")
public class ApprovalController {

    private final TaskCompletionRepository taskCompletionRepository;
    private final ApprovalService approvalService;

    public ApprovalController(TaskCompletionRepository taskCompletionRepository, ApprovalService approvalService) {
        this.taskCompletionRepository = taskCompletionRepository;
        this.approvalService = approvalService;
    }

    @PostMapping("/api/task-completions/{id}/approve")
    public void approve(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        TaskCompletion completion = fetchAndAuthorize(id, user);
        approvalService.approve(completion, user.userId());
    }

    @PostMapping("/api/task-completions/{id}/reject")
    public void reject(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        TaskCompletion completion = fetchAndAuthorize(id, user);
        approvalService.reject(completion, user.userId());
    }

    private TaskCompletion fetchAndAuthorize(UUID id, AuthenticatedUser user) {
        TaskCompletion completion = taskCompletionRepository.findByIdFetchChildAndTask(id)
                .orElseThrow(() -> new DomainException("TASK_NOT_FOUND", HttpStatus.NOT_FOUND, "Sol·licitud no trobada"));
        UUID completionFamilyId = completion.getChild().getUser().getFamily().getId();
        if (!completionFamilyId.equals(user.familyId())) {
            throw new DomainException("ACCESS_DENIED", HttpStatus.FORBIDDEN, "No pots revisar tasques d'una altra família");
        }
        return completion;
    }
}
