package cat.mapaka;

import cat.mapaka.auth.AuthController;
import cat.mapaka.auth.AuthResponse;
import cat.mapaka.auth.RecoverRequest;
import cat.mapaka.auth.RecoverResetPinRequest;
import cat.mapaka.auth.RecoverResponse;
import cat.mapaka.child.ChildDetailResponse;
import cat.mapaka.child.ChildManagementController;
import cat.mapaka.child.ChildProfile;
import cat.mapaka.child.ChildProfileRepository;
import cat.mapaka.child.CreateChildRequest;
import cat.mapaka.common.DomainException;
import cat.mapaka.family.Family;
import cat.mapaka.family.FamilyRegisterRequest;
import cat.mapaka.family.FamilyRegisterResponse;
import cat.mapaka.family.FamilyRegistrationController;
import cat.mapaka.family.FamilyRepository;
import cat.mapaka.security.AuthenticatedUser;
import cat.mapaka.user.User;
import cat.mapaka.user.UserRepository;
import cat.mapaka.user.UserRole;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cobreix el flux de registre de família, alta d'un fill amb PIN, i recuperació de PIN
 * (Prompt 16, punt 9 de la verificació — abans no hi havia cap test d'aquest flux). Crida
 * els controllers directament (mateix patró que la resta de tests d'aquesta suite).
 */
@SpringBootTest
class FamilyRegistrationIntegrationTest {

    static EmbeddedPostgres pg;

    @BeforeAll
    static void startDatabase() throws IOException {
        pg = EmbeddedPostgres.start();
    }

    @AfterAll
    static void stopDatabase() throws IOException {
        pg.close();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> pg.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("JWT_SECRET", () -> "test-secret-0123456789abcdef0123456789abcdef");
        registry.add("FRONTEND_URL", () -> "http://localhost:5173");
    }

    @Autowired FamilyRegistrationController familyRegistrationController;
    @Autowired AuthController authController;
    @Autowired ChildManagementController childManagementController;
    @Autowired FamilyRepository familyRepository;
    @Autowired UserRepository userRepository;
    @Autowired ChildProfileRepository childProfileRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private FamilyRegisterResponse register(String familyName, String parentPin) {
        return familyRegistrationController
                .register(new FamilyRegisterRequest(familyName, "Mama", parentPin, "ca"), new MockHttpServletResponse())
                .getBody();
    }

    private AuthenticatedUser asParent(FamilyRegisterResponse registered) {
        AuthResponse auth = registered.auth();
        return new AuthenticatedUser(auth.userId(), auth.familyId(), UserRole.PARENT, null);
    }

    /** @PreAuthorize necessita una Authentication real al SecurityContext, com la que
     *  JwtAuthenticationFilter hi posaria en una petició HTTP real. */
    private void authenticateAs(AuthenticatedUser user) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, authorities));
    }

    @Test
    @Transactional
    void register_createsFamilyAndFirstParent_recoveryCodeAndPinAreNeverStoredInPlainText() {
        FamilyRegisterResponse registered = register("Test" + UUID.randomUUID(), "1111");

        assertThat(registered.auth().role()).isEqualTo(UserRole.PARENT);
        assertThat(registered.recoveryCode()).isNotBlank();

        Family family = familyRepository.findById(registered.auth().familyId()).orElseThrow();
        assertThat(family.getRecoveryCodeHash()).isNotEqualTo(registered.recoveryCode());
        assertThat(passwordEncoder.matches(registered.recoveryCode(), family.getRecoveryCodeHash())).isTrue();

        User parent = userRepository.findById(registered.auth().userId()).orElseThrow();
        assertThat(parent.getPasswordHash()).isNotEqualTo("1111");
        assertThat(passwordEncoder.matches("1111", parent.getPasswordHash())).isTrue();
    }

    @Test
    @Transactional
    void addChildWithPin_createsChildProfileWithHashedPin_notPlainText() {
        FamilyRegisterResponse registered = register("Test" + UUID.randomUUID(), "1111");
        AuthenticatedUser parent = asParent(registered);
        authenticateAs(parent);

        ChildDetailResponse childResponse = childManagementController
                .create(new CreateChildRequest("Kid", LocalDate.of(2016, 1, 1), null, null, "5678", "ca"), parent)
                .getBody();

        assertThat(childResponse.displayName()).isEqualTo("Kid");
        ChildProfile child = childProfileRepository.findById(childResponse.childId()).orElseThrow();
        User childUser = child.getUser();
        assertThat(childUser.getRole()).isEqualTo(UserRole.CHILD);
        assertThat(childUser.getPasswordHash()).isNotEqualTo("5678");
        assertThat(passwordEncoder.matches("5678", childUser.getPasswordHash())).isTrue();
    }

    @Test
    @Transactional
    void recover_withValidCode_letsResetPinAndConsumesTheCode() {
        FamilyRegisterResponse registered = register("Test" + UUID.randomUUID(), "1111");
        AuthResponse auth = registered.auth();

        RecoverResponse recovered = authController
                .recover(new RecoverRequest(auth.familyId(), registered.recoveryCode())).getBody();
        assertThat(recovered.recoveryToken()).isNotBlank();

        authController.recoverResetPin(new RecoverResetPinRequest(recovered.recoveryToken(), "9999"));

        User parent = userRepository.findById(auth.userId()).orElseThrow();
        assertThat(passwordEncoder.matches("1111", parent.getPasswordHash())).isFalse();
        assertThat(passwordEncoder.matches("9999", parent.getPasswordHash())).isTrue();

        // El codi ja s'ha consumit: tornar-lo a fer servir ha de fallar, encara que el text sigui correcte.
        assertThatThrownBy(() -> authController.recover(new RecoverRequest(auth.familyId(), registered.recoveryCode())))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_RECOVERY_CODE");
    }

    @Test
    @Transactional
    void recover_withWrongCode_isRejectedWithoutConsumingTheRealOne() {
        FamilyRegisterResponse registered = register("Test" + UUID.randomUUID(), "1111");
        AuthResponse auth = registered.auth();

        assertThatThrownBy(() -> authController.recover(new RecoverRequest(auth.familyId(), "WRONGCODE")))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_RECOVERY_CODE");

        // El codi real segueix sent vàlid — un intent fallit no l'ha de consumir.
        RecoverResponse recovered = authController
                .recover(new RecoverRequest(auth.familyId(), registered.recoveryCode())).getBody();
        assertThat(recovered.recoveryToken()).isNotBlank();
    }
}
