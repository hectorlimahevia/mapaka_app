package cat.mapaka.child;

import cat.mapaka.allowance.AllowanceRule;
import cat.mapaka.allowance.AllowanceRuleRepository;
import cat.mapaka.allowance.AllowanceRuleUpdateRequest;
import cat.mapaka.common.DomainException;
import cat.mapaka.screentime.ScreenTimeRule;
import cat.mapaka.screentime.ScreenTimeRuleRepository;
import cat.mapaka.screentime.ScreenTimeRuleUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Gestió de perfil, paga i pantalla per fill (Prompt 7, "Fills"). Canviar una regla no
 * la sobreescriu: desactiva la vigent i n'insereix una de nova, preservant l'històric
 * (mateix principi que el ledger — mai es perd com s'havia calculat una recompensa passada).
 */
@Service
public class ChildManagementService {

    private final ChildProfileRepository childProfileRepository;
    private final AllowanceRuleRepository allowanceRuleRepository;
    private final ScreenTimeRuleRepository screenTimeRuleRepository;

    public ChildManagementService(
            ChildProfileRepository childProfileRepository,
            AllowanceRuleRepository allowanceRuleRepository,
            ScreenTimeRuleRepository screenTimeRuleRepository) {
        this.childProfileRepository = childProfileRepository;
        this.allowanceRuleRepository = allowanceRuleRepository;
        this.screenTimeRuleRepository = screenTimeRuleRepository;
    }

    @Transactional(readOnly = true)
    public List<ChildDetailResponse> detailsFor(UUID familyId) {
        return childProfileRepository.findAllByFamilyId(familyId).stream()
                .map(this::toDetail)
                .toList();
    }

    private ChildDetailResponse toDetail(ChildProfile child) {
        AllowanceRule rule = allowanceRuleRepository.findByChildIdAndActiveTrue(child.getId()).orElse(null);
        ScreenTimeRule screenRule = screenTimeRuleRepository.findByChildIdAndWeekdayIsNullAndActiveTrue(child.getId()).orElse(null);
        return new ChildDetailResponse(
                child.getId(),
                child.getDisplayName(),
                child.getAvatar(),
                child.getAge(),
                rule != null ? rule.getMonthlyAmount() : null,
                rule != null ? rule.getSpendingPercentage() : null,
                rule != null ? rule.getSavingsPercentage() : null,
                screenRule != null ? screenRule.getBaseMinutes() : null);
    }

    @Transactional
    public void updateAllowanceRule(ChildProfile child, AllowanceRuleUpdateRequest request) {
        var sum = request.spendingPercentage().add(request.savingsPercentage());
        if (sum.compareTo(new java.math.BigDecimal("100")) != 0) {
            throw new DomainException("INVALID_ALLOWANCE_RULE", HttpStatus.BAD_REQUEST,
                    "El percentatge de gasto i estalvi han de sumar 100");
        }

        allowanceRuleRepository.findByChildIdAndActiveTrue(child.getId()).ifPresent(previous -> {
            previous.setActive(false);
            previous.setEffectiveTo(LocalDate.now().minusDays(1));
            allowanceRuleRepository.save(previous);
        });

        allowanceRuleRepository.save(AllowanceRule.builder()
                .family(child.getUser().getFamily())
                .child(child)
                .monthlyAmount(request.monthlyAmount())
                .spendingPercentage(request.spendingPercentage())
                .savingsPercentage(request.savingsPercentage())
                .effectiveFrom(LocalDate.now())
                .active(true)
                .build());
    }

    @Transactional
    public void updateScreenTimeRule(ChildProfile child, ScreenTimeRuleUpdateRequest request) {
        screenTimeRuleRepository.findByChildIdAndWeekdayIsNullAndActiveTrue(child.getId()).ifPresent(previous -> {
            previous.setActive(false);
            previous.setValidUntil(LocalDate.now().minusDays(1));
            screenTimeRuleRepository.save(previous);
        });

        screenTimeRuleRepository.save(ScreenTimeRule.builder()
                .child(child)
                .baseMinutes(request.baseMinutes())
                .rolloverEnabled(false)
                .validFrom(LocalDate.now())
                .active(true)
                .build());
    }
}
