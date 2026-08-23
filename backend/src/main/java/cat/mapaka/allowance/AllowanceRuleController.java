package cat.mapaka.allowance;

import cat.mapaka.common.DomainException;
import cat.mapaka.family.Family;
import cat.mapaka.family.FamilyAccessService;
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
public class AllowanceRuleController {

    private final AllowanceRuleService allowanceRuleService;
    private final AllowanceRuleRepository allowanceRuleRepository;
    private final FamilyAccessService familyAccessService;

    public AllowanceRuleController(
            AllowanceRuleService allowanceRuleService,
            AllowanceRuleRepository allowanceRuleRepository,
            FamilyAccessService familyAccessService) {
        this.allowanceRuleService = allowanceRuleService;
        this.allowanceRuleRepository = allowanceRuleRepository;
        this.familyAccessService = familyAccessService;
    }

    @GetMapping("/api/allowance-rules")
    public List<AllowanceRuleResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return allowanceRuleService.list(user.familyId());
    }

    @PostMapping("/api/allowance-rules")
    public ResponseEntity<AllowanceRuleResponse> create(
            @Valid @RequestBody GeneralAllowanceRuleRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        Family family = familyAccessService.requireParentAccess(user.familyId(), user);
        return ResponseEntity.ok(allowanceRuleService.create(family, request));
    }

    @PatchMapping("/api/allowance-rules/{id}")
    public AllowanceRuleResponse update(
            @PathVariable UUID id, @Valid @RequestBody GeneralAllowanceRuleRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        AllowanceRule rule = requireGeneralRuleInFamily(id, user);
        return allowanceRuleService.update(rule, request);
    }

    @DeleteMapping("/api/allowance-rules/{id}")
    public void deactivate(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        AllowanceRule rule = requireGeneralRuleInFamily(id, user);
        allowanceRuleService.deactivate(rule);
    }

    private AllowanceRule requireGeneralRuleInFamily(UUID id, AuthenticatedUser user) {
        AllowanceRule rule = allowanceRuleRepository.findById(id)
                .orElseThrow(() -> new DomainException("ALLOWANCE_RULE_NOT_FOUND", HttpStatus.NOT_FOUND, "Regla de paga no trobada"));
        if (!rule.getFamily().getId().equals(user.familyId())) {
            throw new DomainException("ACCESS_DENIED", HttpStatus.FORBIDDEN, "No pots gestionar una regla d'una altra família");
        }
        if (rule.getChild() != null) {
            throw new DomainException("ALLOWANCE_RULE_NOT_FOUND", HttpStatus.NOT_FOUND, "Regla de paga no trobada");
        }
        return rule;
    }
}
