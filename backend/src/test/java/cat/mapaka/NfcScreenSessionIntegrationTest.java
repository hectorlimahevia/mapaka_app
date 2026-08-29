package cat.mapaka;

import cat.mapaka.child.ChildProfile;
import cat.mapaka.child.ChildProfileRepository;
import cat.mapaka.common.DomainException;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Arrenca el context Spring complet (JPA amb ddl-auto=validate + Flyway) contra un Postgres
 * real, sense Docker: embedded-postgres descarrega i executa un binari natiu de Postgres.
 * Si els ENUMs natius o les columnes de les entitats no encaixen amb les migracions, el
 * context falla a l'arrencar — és la prova definitiva que el mapeig JPA és correcte.
 *
 * Crida el controller (no el servei directament, com abans — Prompt 16, punt 7 de la
 * verificació) perquè els tres endpoints de la sessió NFC (tap, stop, assign) quedin
 * exercits de debò, incloent el cas explícit de repartiment amb saldo negatiu.
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

    @Autowired FamilyRepository familyRepository;
    @Autowired UserRepository userRepository;
    @Autowired ChildProfileRepository childProfileRepository;
    @Autowired ScreenTagRepository screenTagRepository;
    @Autowired NfcScreenSessionController nfcScreenSessionController;
    @Autowired ScreenTimeTransactionRepository screenTimeTransactionRepository;

    private record Fixture(Family family, ChildProfile marti, ChildProfile pau) {}

    private Fixture seed() {
        Family family = familyRepository.save(Family.builder()
                .name("Sande-Lima" + UUID.randomUUID()).currency("EUR").timezone("Europe/Madrid").language("ca").active(true)
                .build());
        userRepository.save(User.builder()
                .family(family).email("parent" + UUID.randomUUID() + "@mapaka.test")
                .passwordHash(new BCryptPasswordEncoder().encode("secret"))
                .role(UserRole.PARENT).active(true)
                .build());
        User childUser1 = userRepository.save(User.builder()
                .family(family).username("marti" + UUID.randomUUID())
                .passwordHash(new BCryptPasswordEncoder().encode("1234"))
                .role(UserRole.CHILD).active(true)
                .build());
        User childUser2 = userRepository.save(User.builder()
                .family(family).username("pau" + UUID.randomUUID())
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
        return new Fixture(family, marti, pau);
    }

    private ScreenTag seedTag(Family family) {
        return screenTagRepository.save(ScreenTag.builder()
                .family(family).token("tag-" + UUID.randomUUID()).active(true)
                .build());
    }

    @Test
    @Transactional
    void tapStartsAndStopsASession_andAssignSplitsMinutesBetweenChildren() throws InterruptedException {
        Fixture f = seed();
        ScreenTag tag = seedTag(f.family);

        ScreenSessionStatusResponse started = nfcScreenSessionController.tap(tag.getToken());
        assertThat(started.status()).isEqualTo(ScreenSessionStatus.ACTIVE);

        Thread.sleep(1100);

        ScreenSessionStatusResponse stopped = nfcScreenSessionController.tap(tag.getToken());
        assertThat(stopped.status()).isEqualTo(ScreenSessionStatus.CLOSED);
        assertThat(stopped.elapsedSeconds()).isGreaterThanOrEqualTo(1);
        assertThat(stopped.familyChildren()).hasSize(2);

        AssignSessionResponse assigned = nfcScreenSessionController.assign(
                started.sessionId(), new AssignSessionRequest(List.of(f.marti.getId(), f.pau.getId())));

        assertThat(assigned.participants()).hasSize(2);
        int totalAssignedSeconds = assigned.participants().stream()
                .mapToInt(AssignSessionResponse.ParticipantResult::assignedSeconds).sum();
        assertThat(totalAssignedSeconds).isEqualTo(stopped.elapsedSeconds());

        int martiBalance = screenTimeTransactionRepository.balanceFor(f.marti.getId());
        assertThat(martiBalance).isLessThanOrEqualTo(0);
    }

    @Test
    @Transactional
    void stopEndpoint_closesActiveSession_andRejectsBeingCalledTwice() {
        Fixture f = seed();
        ScreenTag tag = seedTag(f.family);

        ScreenSessionStatusResponse started = nfcScreenSessionController.tap(tag.getToken());
        assertThat(started.status()).isEqualTo(ScreenSessionStatus.ACTIVE);

        ScreenSessionStatusResponse stopped = nfcScreenSessionController.stop(started.sessionId());
        assertThat(stopped.status()).isEqualTo(ScreenSessionStatus.CLOSED);
        assertThat(stopped.elapsedSeconds()).isNotNull();

        assertThatThrownBy(() -> nfcScreenSessionController.stop(started.sessionId()))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SESSION_ALREADY_CLOSED");
    }

    @Test
    @Transactional
    void tap_unknownOrInactiveTag_throwsNotFound() {
        assertThatThrownBy(() -> nfcScreenSessionController.tap("does-not-exist"))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SCREEN_TAG_NOT_FOUND");
    }

    @Test
    @Transactional
    void assign_beforeSessionIsStopped_throwsSessionStillActive() {
        Fixture f = seed();
        ScreenTag tag = seedTag(f.family);
        ScreenSessionStatusResponse started = nfcScreenSessionController.tap(tag.getToken());

        assertThatThrownBy(() -> nfcScreenSessionController.assign(
                started.sessionId(), new AssignSessionRequest(List.of(f.marti.getId()))))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "SESSION_STILL_ACTIVE");
    }

    @Test
    @Transactional
    void assign_whenChildHasNoMinutesLeft_resultsInNegativeBalance() throws InterruptedException {
        // Cas explícit de repartiment amb saldo negatiu (Prompt 16, punt 7): el fill no té
        // cap minut acreditat, així que qualsevol repartiment el deixa en negatiu — es
        // recupera amb la propera paga/bonificació de temps, com ja fa el ledger de diners.
        Fixture f = seed();
        ScreenTag tag = seedTag(f.family);

        ScreenSessionStatusResponse started = nfcScreenSessionController.tap(tag.getToken());
        Thread.sleep(1100);
        nfcScreenSessionController.tap(tag.getToken());

        AssignSessionResponse assigned = nfcScreenSessionController.assign(
                started.sessionId(), new AssignSessionRequest(List.of(f.marti.getId())));

        AssignSessionResponse.ParticipantResult result = assigned.participants().get(0);
        assertThat(result.negativeBalance()).isTrue();
        assertThat(result.resultingBalanceMinutes()).isLessThan(0);
        assertThat(screenTimeTransactionRepository.balanceFor(f.marti.getId())).isLessThan(0);
    }
}
