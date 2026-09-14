package cat.mapaka.screentime;

import cat.mapaka.family.Family;
import cat.mapaka.family.FamilyAccessService;
import cat.mapaka.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class NfcScreenSessionController {

    private final NfcScreenSessionService service;
    private final FamilyAccessService familyAccessService;

    public NfcScreenSessionController(NfcScreenSessionService service, FamilyAccessService familyAccessService) {
        this.service = service;
        this.familyAccessService = familyAccessService;
    }

    @PostMapping("/api/screen-tags/{token}/tap")
    public ScreenSessionStatusResponse tap(@PathVariable String token) {
        return service.tap(token);
    }

    @PostMapping("/api/screen-sessions/{id}/stop")
    public ScreenSessionStatusResponse stop(@PathVariable UUID id) {
        return service.stop(id);
    }

    @PostMapping("/api/screen-sessions/{id}/assign")
    public AssignSessionResponse assign(@PathVariable UUID id, @Valid @RequestBody AssignSessionRequest request) {
        return service.assign(id, request);
    }

    @PreAuthorize("hasRole('PARENT')")
    @PostMapping("/api/families/{familyId}/screen-sessions/start")
    public ScreenSessionStatusResponse startManual(@PathVariable UUID familyId, @AuthenticationPrincipal AuthenticatedUser user) {
        Family family = familyAccessService.requireParentAccess(familyId, user);
        return service.startManual(family, user.userId());
    }

    @PreAuthorize("hasRole('PARENT')")
    @GetMapping("/api/families/{familyId}/screen-sessions/active")
    public ResponseEntity<ScreenSessionStatusResponse> activeManualSession(@PathVariable UUID familyId, @AuthenticationPrincipal AuthenticatedUser user) {
        familyAccessService.requireParentAccess(familyId, user);
        return service.activeManualSession(familyId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
