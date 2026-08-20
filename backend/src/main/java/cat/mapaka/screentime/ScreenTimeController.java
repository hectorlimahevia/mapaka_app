package cat.mapaka.screentime;

import cat.mapaka.child.ChildAccessService;
import cat.mapaka.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ScreenTimeController {

    private final ChildAccessService childAccessService;
    private final ScreenTimeService screenTimeService;

    public ScreenTimeController(ChildAccessService childAccessService, ScreenTimeService screenTimeService) {
        this.childAccessService = childAccessService;
        this.screenTimeService = screenTimeService;
    }

    @GetMapping("/api/children/{id}/screen-time/today")
    public ScreenTimeStatusResponse today(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        var child = childAccessService.requireAccess(id, user);
        return screenTimeService.getTodayStatus(child);
    }
}
