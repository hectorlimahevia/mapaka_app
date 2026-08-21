package cat.mapaka;

import cat.mapaka.family.Family;
import cat.mapaka.family.FamilyRepository;
import cat.mapaka.screentime.ScreenTagAdminController;
import cat.mapaka.screentime.ScreenTagResponse;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ScreenTagAdminIntegrationTest {

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

    @Autowired FamilyRepository familyRepository;
    @Autowired UserRepository userRepository;
    @Autowired ScreenTagAdminController screenTagAdminController;

    @Test
    @Transactional
    void createGeneratesUniqueTokenAndListReturnsIt() {
        Family family = familyRepository.save(Family.builder()
                .name("Test Family").currency("EUR").timezone("Europe/Madrid").language("ca").active(true)
                .taskApprovalRequired(true).notifyPendingApprovalsEnabled(false).allowSavingsTransfer(true)
                .build());
        User parentUser = userRepository.save(User.builder()
                .family(family).email("p" + UUID.randomUUID() + "@test.com")
                .passwordHash(new BCryptPasswordEncoder().encode("x")).role(UserRole.PARENT).active(true)
                .build());
        AuthenticatedUser parent = new AuthenticatedUser(parentUser.getId(), family.getId(), UserRole.PARENT, null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                parent, null, List.of(new SimpleGrantedAuthority("ROLE_PARENT"))));

        ScreenTagResponse first = screenTagAdminController.create(family.getId(), parent);
        ScreenTagResponse second = screenTagAdminController.create(family.getId(), parent);

        assertThat(first.token()).hasSize(12);
        assertThat(first.token()).isNotEqualTo(second.token());
        assertThat(first.active()).isTrue();

        List<ScreenTagResponse> list = screenTagAdminController.list(family.getId(), parent);
        assertThat(list).extracting(ScreenTagResponse::token).containsExactlyInAnyOrder(first.token(), second.token());

        SecurityContextHolder.clearContext();
    }
}
