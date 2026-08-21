package cat.mapaka.screentime;

import cat.mapaka.child.ChildAccessService;
import cat.mapaka.family.FamilyAccessService;
import cat.mapaka.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ScreenTimeController {

    private final ChildAccessService childAccessService;
    private final FamilyAccessService familyAccessService;
    private final ScreenTimeService screenTimeService;
    private final ScreenSessionParticipantRepository screenSessionParticipantRepository;

    public ScreenTimeController(
            ChildAccessService childAccessService,
            FamilyAccessService familyAccessService,
            ScreenTimeService screenTimeService,
            ScreenSessionParticipantRepository screenSessionParticipantRepository) {
        this.childAccessService = childAccessService;
        this.familyAccessService = familyAccessService;
        this.screenTimeService = screenTimeService;
        this.screenSessionParticipantRepository = screenSessionParticipantRepository;
    }

    @GetMapping("/api/children/{id}/screen-time/today")
    public ScreenTimeStatusResponse today(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        var child = childAccessService.requireAccess(id, user);
        return screenTimeService.getTodayStatus(child);
    }

    /** Informatiu per a "Aprovacions" (Prompt 7) — repartiments NFC que han deixat algun fill en negatiu. */
    @GetMapping("/api/families/{id}/screen-sessions/negative-balance")
    public List<NegativeBalanceSessionResponse> negativeBalanceSessions(
            @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        familyAccessService.requireParentAccess(id, user);
        return screenSessionParticipantRepository.findNegativeBalanceByFamilyId(id).stream()
                .map(NegativeBalanceSessionResponse::from)
                .toList();
    }
}
