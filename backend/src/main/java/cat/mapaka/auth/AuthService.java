package cat.mapaka.auth;

import cat.mapaka.child.ChildProfileRepository;
import cat.mapaka.common.DomainException;
import cat.mapaka.security.AuthenticatedUser;
import cat.mapaka.security.JwtService;
import cat.mapaka.user.User;
import cat.mapaka.user.UserRepository;
import cat.mapaka.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ChildProfileRepository childProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            ChildProfileRepository childProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.childProfileRepository = childProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        User user = resolveUser(request);

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new DomainException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Credencials incorrectes");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        AuthenticatedUser principal = toAuthenticatedUser(user);
        return issueTokens(principal);
    }

    public LoginResult refresh(String refreshToken) {
        UUID userId = jwtService.parseRefreshToken(refreshToken);
        User user = userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new DomainException("INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "Token no vàlid"));
        return issueTokens(toAuthenticatedUser(user));
    }

    /** Emet un parell de tokens per a un usuari ja creat (registre de família, alta de PARENT
     * addicional) sense passar per la comprovació de contrasenya de login(). */
    @Transactional
    public LoginResult issueTokensForUser(User user) {
        return issueTokens(toAuthenticatedUser(user));
    }

    private LoginResult issueTokens(AuthenticatedUser principal) {
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal.userId());
        String displayName = principal.childId() == null ? null
                : childProfileRepository.findById(principal.childId())
                        .map(cat.mapaka.child.ChildProfile::getDisplayName)
                        .orElse(null);
        AuthResponse response = new AuthResponse(
                accessToken, principal.userId(), principal.familyId(), principal.role(), principal.childId(), displayName);
        return new LoginResult(response, refreshToken);
    }

    private User resolveUser(LoginRequest request) {
        if (request.email() != null && !request.email().isBlank()) {
            return userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new DomainException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Credencials incorrectes"));
        }
        if (request.familyId() != null && request.username() != null) {
            return userRepository.findByFamilyIdAndUsername(request.familyId(), request.username())
                    .orElseThrow(() -> new DomainException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Credencials incorrectes"));
        }
        throw new DomainException("INVALID_LOGIN_REQUEST", HttpStatus.BAD_REQUEST,
                "Cal indicar email+password (adult) o familyId+username+PIN (fill)");
    }

    private AuthenticatedUser toAuthenticatedUser(User user) {
        UUID childId = null;
        if (user.getRole() == UserRole.CHILD) {
            childId = childProfileRepository.findByUserId(user.getId())
                    .map(cat.mapaka.child.ChildProfile::getId)
                    .orElse(null);
        }
        return new AuthenticatedUser(user.getId(), user.getFamily().getId(), user.getRole(), childId);
    }

    public record LoginResult(AuthResponse response, String refreshToken) {
    }
}
