package cat.mapaka.child;

import cat.mapaka.allowance.AllowanceRule;
import cat.mapaka.allowance.AllowanceRuleRepository;
import cat.mapaka.allowance.AllowanceRuleService;
import cat.mapaka.allowance.AllowanceRuleUpdateRequest;
import cat.mapaka.common.DomainException;
import cat.mapaka.family.Family;
import cat.mapaka.screentime.ScreenTimeRule;
import cat.mapaka.screentime.ScreenTimeRuleRepository;
import cat.mapaka.screentime.ScreenTimeRuleUpdateRequest;
import cat.mapaka.user.User;
import cat.mapaka.user.UserRepository;
import cat.mapaka.user.UserRole;
import cat.mapaka.user.UsernameAllocator;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final AllowanceRuleService allowanceRuleService;
    private final ScreenTimeRuleRepository screenTimeRuleRepository;
    private final UserRepository userRepository;
    private final UsernameAllocator usernameAllocator;
    private final PasswordEncoder passwordEncoder;

    public ChildManagementService(
            ChildProfileRepository childProfileRepository,
            AllowanceRuleRepository allowanceRuleRepository,
            AllowanceRuleService allowanceRuleService,
            ScreenTimeRuleRepository screenTimeRuleRepository,
            UserRepository userRepository,
            UsernameAllocator usernameAllocator,
            PasswordEncoder passwordEncoder) {
        this.childProfileRepository = childProfileRepository;
        this.allowanceRuleRepository = allowanceRuleRepository;
        this.allowanceRuleService = allowanceRuleService;
        this.screenTimeRuleRepository = screenTimeRuleRepository;
        this.userRepository = userRepository;
        this.usernameAllocator = usernameAllocator;
        this.passwordEncoder = passwordEncoder;
    }

    /** Alta d'un fill (Prompt 6) — crea l'usuari CHILD (username derivat del nom, PIN hashejat
     * igual que un password) i el seu perfil en una única transacció. */
    @Transactional
    public ChildDetailResponse createChild(Family family, CreateChildRequest request) {
        if (!ChildColorPalette.isValid(request.colorTheme())) {
            throw new DomainException("INVALID_CHILD_COLOR", HttpStatus.BAD_REQUEST,
                    "El color triat no forma part de la paleta permesa");
        }
        User user = userRepository.save(User.builder()
                .family(family)
                .username(usernameAllocator.allocate(family.getId(), request.displayName()))
                .passwordHash(passwordEncoder.encode(request.pin()))
                .role(UserRole.CHILD)
                .active(true)
                .locale(request.locale() != null ? request.locale() : "ca")
                .build());

        ChildProfile child = childProfileRepository.save(ChildProfile.builder()
                .user(user)
                .displayName(request.displayName())
                .birthDate(request.birthDate())
                .avatar(request.avatar())
                .colorTheme(request.colorTheme())
                .allowanceEnabled(true)
                .screenTimeEnabled(true)
                .active(true)
                .build());

        return toDetail(child);
    }

    @Transactional(readOnly = true)
    public List<ChildDetailResponse> detailsFor(UUID familyId) {
        return childProfileRepository.findAllByFamilyId(familyId).stream()
                .map(this::toDetail)
                .toList();
    }

    private ChildDetailResponse toDetail(ChildProfile child) {
        boolean hasCustomAllowance = allowanceRuleRepository.findByChildIdAndActiveTrue(child.getId()).isPresent();
        AllowanceRule effectiveRule = allowanceRuleService.resolveEffectiveRule(child).orElse(null);
        ScreenTimeRule screenRule = screenTimeRuleRepository.findByChildIdAndWeekdayIsNullAndActiveTrue(child.getId()).orElse(null);
        return new ChildDetailResponse(
                child.getId(),
                child.getDisplayName(),
                child.getAvatar(),
                child.getAge(),
                hasCustomAllowance,
                effectiveRule != null ? effectiveRule.getMonthlyAmount() : null,
                effectiveRule != null ? effectiveRule.getSpendingPercentage() : null,
                effectiveRule != null ? effectiveRule.getSavingsPercentage() : null,
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

    /** Desactiva l'interruptor "Paga personalitzada": el fill torna a dependre de la regla
     * general per edat, sense esborrar l'històric de la seva regla anterior. */
    @Transactional
    public void clearAllowanceRule(ChildProfile child) {
        allowanceRuleRepository.findByChildIdAndActiveTrue(child.getId()).ifPresent(previous -> {
            previous.setActive(false);
            previous.setEffectiveTo(LocalDate.now().minusDays(1));
            allowanceRuleRepository.save(previous);
        });
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
