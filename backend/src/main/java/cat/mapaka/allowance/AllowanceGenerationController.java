package cat.mapaka.allowance;

import cat.mapaka.common.DomainException;
import cat.mapaka.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('PARENT')")
public class AllowanceGenerationController {

    private final AllowanceGenerationService allowanceGenerationService;
    private final MonthlyAllowanceRepository monthlyAllowanceRepository;

    public AllowanceGenerationController(
            AllowanceGenerationService allowanceGenerationService, MonthlyAllowanceRepository monthlyAllowanceRepository) {
        this.allowanceGenerationService = allowanceGenerationService;
        this.monthlyAllowanceRepository = monthlyAllowanceRepository;
    }

    /** Alimenta la campaneta d'alertes de Resum familiar (Prompt 15, checklist 18) —
     * cap taula/camp nou, es dedueix de si ja existeix cap MonthlyAllowance aquest mes. */
    @GetMapping("/api/families/{id}/allowance-status")
    public AllowanceStatusResponse allowanceStatus(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        if (!id.equals(user.familyId())) {
            throw new DomainException("ACCESS_DENIED", HttpStatus.FORBIDDEN, "No pots administrar una altra família");
        }
        LocalDate today = LocalDate.now();
        boolean generatedThisMonth = monthlyAllowanceRepository
                .existsByChild_User_Family_IdAndYearAndMonth(id, today.getYear(), today.getMonthValue());
        return new AllowanceStatusResponse(generatedThisMonth);
    }

    @PostMapping("/api/allowances/generate")
    public List<MonthlyAllowanceResponse> generate(@AuthenticationPrincipal AuthenticatedUser user) {
        return allowanceGenerationService.generate(user.familyId());
    }

    @PostMapping("/api/allowances/{id}/confirm")
    public MonthlyAllowanceResponse confirm(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        MonthlyAllowance allowance = requireAllowanceInFamily(id, user);
        return allowanceGenerationService.confirm(allowance, user.userId());
    }

    @PostMapping("/api/allowances/{id}/cancel")
    public MonthlyAllowanceResponse cancel(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        MonthlyAllowance allowance = requireAllowanceInFamily(id, user);
        return allowanceGenerationService.cancel(allowance);
    }

    private MonthlyAllowance requireAllowanceInFamily(UUID id, AuthenticatedUser user) {
        MonthlyAllowance allowance = monthlyAllowanceRepository.findByIdFetchFamily(id)
                .orElseThrow(() -> new DomainException("ALLOWANCE_NOT_FOUND", HttpStatus.NOT_FOUND, "Paga no trobada"));
        if (!allowance.getChild().getUser().getFamily().getId().equals(user.familyId())) {
            throw new DomainException("ACCESS_DENIED", HttpStatus.FORBIDDEN, "No pots gestionar la paga d'una altra família");
        }
        return allowance;
    }
}
