package cat.mapaka;

import cat.mapaka.child.ChildProfile;
import cat.mapaka.child.ChildProfileRepository;
import cat.mapaka.family.Family;
import cat.mapaka.family.FamilyRepository;
import cat.mapaka.screentime.*;
import cat.mapaka.user.User;
import cat.mapaka.user.UserRepository;
import cat.mapaka.user.UserRole;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Arrenca el context Spring complet (JPA amb ddl-auto=validate + Flyway) contra un Postgres
 * real, sense Docker: embedded-postgres descarrega i executa un binari natiu de Postgres.
 * Si els ENUMs natius o les columnes de les entitats no encaixen amb les migracions, el
 * context falla a l'arrencar — és la prova definitiva que el mapeig JPA és correcte.
 */
@SpringBootTest
class NfcScreenSessionIntegrationTest {

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

    @Autowired
    FamilyRepository familyRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ChildProfileRepository childProfileRepository;
    @Autowired
    ScreenTagRepository screenTagRepository;
    @Autowired
    NfcScreenSessionService nfcScreenSessionService;
    @Autowired
    ScreenTimeTransactionRepository screenTimeTransactionRepository;

    @Test
    @Transactional
    void tapStartsAndStopsASession_andAssignSplitsMinutesBetweenChildren() throws InterruptedException {
        Family family = familyRepository.save(Family.builder()
                .name("Sande-Lima").currency("EUR").timezone("Europe/Madrid").language("ca").active(true)
                .build());

        User parentUser = userRepository.save(User.builder()
                .family(family).email("parent@mapaka.test")
                .passwordHash(new BCryptPasswordEncoder().encode("secret"))
                .role(UserRole.PARENT).active(true)
                .build());

        User childUser1 = userRepository.save(User.builder()
                .family(family).username("marti")
                .passwordHash(new BCryptPasswordEncoder().encode("1234"))
                .role(UserRole.CHILD).active(true)
                .build());
        User childUser2 = userRepository.save(User.builder()
                .family(family).username("pau")
                .passwordHash(new BCryptPasswordEncoder().encode("1234"))
                .role(UserRole.CHILD).active(true)
                .build());

        ChildProfile marti = childProfileRepository.save(ChildProfile.builder()
                .user(childUser1).displayName("Martí").birthDate(LocalDate.of(2016, 5, 1))
                .allowanceEnabled(true).screenTimeEnabled(true).active(true)
                .build());
        ChildProfile pau = childProfileRepository.save(ChildProfile.builder()
                .user(childUser2).displayName("Pau").birthDate(LocalDate.of(2018, 3, 1))
                .allowanceEnabled(true).screenTimeEnabled(true).active(true)
                .build());

        ScreenTag tag = screenTagRepository.save(ScreenTag.builder()
                .family(family).token("tag-integration-test").active(true)
                .build());

        ScreenSessionStatusResponse started = nfcScreenSessionService.tap(tag.getToken());
        assertThat(started.status()).isEqualTo(ScreenSessionStatus.ACTIVE);

        Thread.sleep(1100);

        ScreenSessionStatusResponse stopped = nfcScreenSessionService.tap(tag.getToken());
        assertThat(stopped.status()).isEqualTo(ScreenSessionStatus.CLOSED);
        assertThat(stopped.elapsedSeconds()).isGreaterThanOrEqualTo(1);
        assertThat(stopped.familyChildren()).hasSize(2);

        AssignSessionResponse assigned = nfcScreenSessionService.assign(
                started.sessionId(), new AssignSessionRequest(java.util.List.of(marti.getId(), pau.getId())));

        assertThat(assigned.participants()).hasSize(2);
        int totalAssignedSeconds = assigned.participants().stream()
                .mapToInt(AssignSessionResponse.ParticipantResult::assignedSeconds).sum();
        assertThat(totalAssignedSeconds).isEqualTo(stopped.elapsedSeconds());

        int martiBalance = screenTimeTransactionRepository.balanceFor(marti.getId());
        assertThat(martiBalance).isLessThanOrEqualTo(0);
    }
}
