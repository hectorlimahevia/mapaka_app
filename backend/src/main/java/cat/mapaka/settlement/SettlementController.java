package cat.mapaka.settlement;

import cat.mapaka.common.DomainException;
import cat.mapaka.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('PARENT')")
public class SettlementController {

    private final MonthlySettlementRepository monthlySettlementRepository;

    public SettlementController(MonthlySettlementRepository monthlySettlementRepository) {
        this.monthlySettlementRepository = monthlySettlementRepository;
    }

    @GetMapping("/api/settlements")
    public List<MonthlySettlementResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return monthlySettlementRepository.findByFamilyId(user.familyId()).stream()
                .map(MonthlySettlementResponse::from)
                .toList();
    }

    @GetMapping("/api/settlements/{id}")
    public MonthlySettlementResponse detail(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        MonthlySettlement settlement = monthlySettlementRepository.findByIdFetchFamily(id)
                .orElseThrow(() -> new DomainException("SETTLEMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "Tancament no trobat"));
        if (!settlement.getChild().getUser().getFamily().getId().equals(user.familyId())) {
            throw new DomainException("ACCESS_DENIED", HttpStatus.FORBIDDEN, "No pots consultar un tancament d'una altra família");
        }
        return MonthlySettlementResponse.from(settlement);
    }
}
