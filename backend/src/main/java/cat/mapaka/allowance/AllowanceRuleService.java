package cat.mapaka.allowance;

import cat.mapaka.child.ChildProfile;
import cat.mapaka.common.DomainException;
import cat.mapaka.family.Family;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Regles de paga generals per franja d'edat (sense child_id) — Prompt 9 ampliat,
 * secció 8 de mapaka_documento_global.md. Mateix criteri d'històric que les regles
 * personalitzades: canviar-ne una no la sobreescriu, en desactiva la vigent i n'insereix
 * una de nova. */
@Service
public class AllowanceRuleService {

    private final AllowanceRuleRepository allowanceRuleRepository;

    public AllowanceRuleService(AllowanceRuleRepository allowanceRuleRepository) {
        this.allowanceRuleRepository = allowanceRuleRepository;
    }

    @Transactional(readOnly = true)
    public List<AllowanceRuleResponse> list(java.util.UUID familyId) {
        return allowanceRuleRepository.findByFamilyIdAndChildIsNullAndActiveTrueOrderByMinAgeAsc(familyId).stream()
                .map(AllowanceRuleResponse::from)
                .toList();
    }

    @Transactional
    public AllowanceRuleResponse create(Family family, GeneralAllowanceRuleRequest request) {
        validate(request);
        AllowanceRule rule = allowanceRuleRepository.save(AllowanceRule.builder()
                .family(family)
                .child(null)
                .minAge(request.minAge())
                .maxAge(request.maxAge())
                .monthlyAmount(request.monthlyAmount())
                .spendingPercentage(request.spendingPercentage())
                .savingsPercentage(request.savingsPercentage())
                .effectiveFrom(LocalDate.now())
                .active(true)
                .build());
        return AllowanceRuleResponse.from(rule);
    }

    @Transactional
    public AllowanceRuleResponse update(AllowanceRule existing, GeneralAllowanceRuleRequest request) {
        validate(request);
        existing.setActive(false);
        existing.setEffectiveTo(LocalDate.now().minusDays(1));
        allowanceRuleRepository.save(existing);

        AllowanceRule rule = allowanceRuleRepository.save(AllowanceRule.builder()
                .family(existing.getFamily())
                .child(null)
                .minAge(request.minAge())
                .maxAge(request.maxAge())
                .monthlyAmount(request.monthlyAmount())
                .spendingPercentage(request.spendingPercentage())
                .savingsPercentage(request.savingsPercentage())
                .effectiveFrom(LocalDate.now())
                .active(true)
                .build());
        return AllowanceRuleResponse.from(rule);
    }

    @Transactional
    public void deactivate(AllowanceRule rule) {
        rule.setActive(false);
        rule.setEffectiveTo(LocalDate.now().minusDays(1));
        allowanceRuleRepository.save(rule);
    }

    /** Percentatge de gastar vigent per a un fill: la seva regla personalitzada si en té,
     * si no la regla general que correspongui per edat. Si no hi ha cap regla configurada,
     * per defecte tot va a gastar (100%) — no bloqueja aprovar tasques ni ajustos en una
     * família que encara no ha definit cap regla de paga. */
    @Transactional(readOnly = true)
    public BigDecimal resolveSpendingPercentage(ChildProfile child) {
        return resolveEffectiveRule(child).map(AllowanceRule::getSpendingPercentage).orElse(new BigDecimal("100"));
    }

    /** Regla vigent per a un fill: la seva personalitzada si en té, si no la general que
     * correspongui per edat (Fills, bloc de només lectura quan l'interruptor és desactivat). */
    @Transactional(readOnly = true)
    public Optional<AllowanceRule> resolveEffectiveRule(ChildProfile child) {
        return allowanceRuleRepository.findByChildIdAndActiveTrue(child.getId())
                .or(() -> resolveGeneralRule(child.getUser().getFamily().getId(), child.getAge()));
    }

    private Optional<AllowanceRule> resolveGeneralRule(UUID familyId, int age) {
        return allowanceRuleRepository.findByFamilyIdAndChildIsNullAndActiveTrueOrderByMinAgeAsc(familyId).stream()
                .filter(r -> r.getMinAge() == null || age >= r.getMinAge())
                .filter(r -> r.getMaxAge() == null || age <= r.getMaxAge())
                .findFirst();
    }

    private void validate(GeneralAllowanceRuleRequest request) {
        if (request.minAge() > request.maxAge()) {
            throw new DomainException("INVALID_ALLOWANCE_RULE", HttpStatus.BAD_REQUEST,
                    "L'edat mínima no pot ser superior a la màxima");
        }
        var sum = request.spendingPercentage().add(request.savingsPercentage());
        if (sum.compareTo(new BigDecimal("100")) != 0) {
            throw new DomainException("INVALID_ALLOWANCE_RULE", HttpStatus.BAD_REQUEST,
                    "El percentatge de gastar i estalviar ha de sumar 100");
        }
    }
}
