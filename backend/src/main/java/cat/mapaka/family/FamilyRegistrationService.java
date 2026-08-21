package cat.mapaka.family;

import cat.mapaka.auth.AuthService;
import cat.mapaka.auth.RecoverRequest;
import cat.mapaka.auth.RecoverResetPinRequest;
import cat.mapaka.auth.RecoverResponse;
import cat.mapaka.common.DomainException;
import cat.mapaka.security.AuthenticatedUser;
import cat.mapaka.security.JwtService;
import cat.mapaka.user.User;
import cat.mapaka.user.UserRepository;
import cat.mapaka.user.UserRole;
import cat.mapaka.user.UsernameAllocator;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Registre públic de família, alta de PARENT addicional i recuperació de PIN — flux dissenyat
 * des de zero perquè Família+.pdf mai el va definir (mapaka_documento_global.md secció 4).
 */
@Service
public class FamilyRegistrationService {

    private static final String RECOVERY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;
    private final UsernameAllocator usernameAllocator;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthService authService;

    public FamilyRegistrationService(
            FamilyRepository familyRepository,
            UserRepository userRepository,
            UsernameAllocator usernameAllocator,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthService authService) {
        this.familyRepository = familyRepository;
        this.userRepository = userRepository;
        this.usernameAllocator = usernameAllocator;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authService = authService;
    }

    @Transactional
    public RegisterResult register(FamilyRegisterRequest request) {
        Family family = familyRepository.save(Family.builder()
                .name(request.familyName())
                .currency("EUR")
                .timezone("Europe/Madrid")
                .language("ca")
                .active(true)
                .taskApprovalRequired(true)
                .notifyPendingApprovalsEnabled(false)
                .allowSavingsTransfer(true)
                .build());

        User parent = userRepository.save(User.builder()
                .family(family)
                .username(usernameAllocator.allocate(family.getId(), request.parentDisplayName()))
                .displayName(request.parentDisplayName())
                .passwordHash(passwordEncoder.encode(request.parentPin()))
                .role(UserRole.PARENT)
                .active(true)
                .locale(request.locale() != null ? request.locale() : "ca")
                .build());

        String recoveryCode = generateRecoveryCode();
        family.setRecoveryCodeHash(passwordEncoder.encode(recoveryCode));
        family.setRecoveryCodeGeneratedAt(Instant.now());
        familyRepository.save(family);

        AuthService.LoginResult loginResult = authService.issueTokensForUser(parent);
        return new RegisterResult(new FamilyRegisterResponse(loginResult.response(), recoveryCode), loginResult.refreshToken());
    }

    public record RegisterResult(FamilyRegisterResponse body, String refreshToken) {
    }

    @Transactional
    public void addParent(AddParentRequest request, AuthenticatedUser actingUser) {
        Family family = familyRepository.findById(actingUser.familyId())
                .orElseThrow(() -> new DomainException("FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND, "Família no trobada"));
        userRepository.save(User.builder()
                .family(family)
                .username(usernameAllocator.allocate(family.getId(), request.displayName()))
                .displayName(request.displayName())
                .passwordHash(passwordEncoder.encode(request.pin()))
                .role(UserRole.PARENT)
                .active(true)
                .locale(request.locale() != null ? request.locale() : "ca")
                .build());
    }

    /** Consumeix el codi (el posa a null) i retorna un token temporal de reset — mai torna a
     * exposar el codi ni permet re-fer-lo servir. */
    @Transactional
    public RecoverResponse recover(RecoverRequest request) {
        Family family = familyRepository.findById(request.familyId())
                .filter(f -> f.getRecoveryCodeHash() != null
                        && passwordEncoder.matches(request.recoveryCode(), f.getRecoveryCodeHash()))
                .orElseThrow(() -> new DomainException("INVALID_RECOVERY_CODE", HttpStatus.UNAUTHORIZED, "Codi de recuperació no vàlid"));

        User firstParent = userRepository.findFirstByFamilyIdAndRoleOrderByCreatedAtAsc(family.getId(), UserRole.PARENT)
                .orElseThrow(() -> new DomainException("NO_PARENT_FOUND", HttpStatus.CONFLICT, "No hi ha cap PARENT a la família"));

        family.setRecoveryCodeHash(null);
        familyRepository.save(family);

        return new RecoverResponse(jwtService.generateRecoveryToken(firstParent.getId(), family.getId()));
    }

    @Transactional
    public void resetPinWithRecoveryToken(RecoverResetPinRequest request) {
        JwtService.RecoveryClaims claims;
        try {
            claims = jwtService.parseRecoveryToken(request.recoveryToken());
        } catch (RuntimeException e) {
            throw new DomainException("INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "Token de recuperació no vàlid o caducat");
        }
        User user = userRepository.findById(claims.userId())
                .filter(u -> u.getFamily().getId().equals(claims.familyId()))
                .orElseThrow(() -> new DomainException("INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "Token de recuperació no vàlid"));
        user.setPasswordHash(passwordEncoder.encode(request.newPin()));
        userRepository.save(user);
    }

    private String generateRecoveryCode() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(RECOVERY_ALPHABET.charAt(RANDOM.nextInt(RECOVERY_ALPHABET.length())));
        }
        return sb.toString();
    }
}
