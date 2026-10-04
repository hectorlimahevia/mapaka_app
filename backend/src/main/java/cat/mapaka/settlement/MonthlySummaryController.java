package cat.mapaka.settlement;

import cat.mapaka.security.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@PreAuthorize("hasRole('PARENT')")
public class MonthlySummaryController {

    private final MonthlySummaryService monthlySummaryService;

    public MonthlySummaryController(MonthlySummaryService monthlySummaryService) {
        this.monthlySummaryService = monthlySummaryService;
    }

    @GetMapping("/api/monthly-summaries")
    public List<MonthlySummaryResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return monthlySummaryService.forFamily(user.familyId());
    }
}
