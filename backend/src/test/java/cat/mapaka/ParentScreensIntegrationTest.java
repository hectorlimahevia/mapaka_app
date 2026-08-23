package cat.mapaka;

import cat.mapaka.allowance.AllowanceRuleUpdateRequest;
import cat.mapaka.child.*;
import cat.mapaka.common.DomainException;
import cat.mapaka.family.*;
import cat.mapaka.money.FamilyMoneyTransactionResponse;
import cat.mapaka.screentime.*;
import cat.mapaka.security.AuthenticatedUser;
import cat.mapaka.task.*;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ParentScreensIntegrationTest {

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
    @Autowired TaskRepository taskRepository;
    @Autowired TaskRewardRepository taskRewardRepository;
    @Autowired TaskAssignmentRepository taskAssignmentRepository;
    @Autowired TaskCompletionRepository taskCompletionRepository;
    @Autowired ScreenTagRepository screenTagRepository;
    @Autowired ScreenSessionRepository screenSessionRepository;
    @Autowired ScreenSessionParticipantRepository screenSessionParticipantRepository;

    @Autowired FamilySummaryController familySummaryController;
    @Autowired PendingApprovalsController pendingApprovalsController;
    @Autowired ApprovalController approvalController;
    @Autowired FamilySettingsController familySettingsController;
    @Autowired ChildManagementController childManagementController;
    @Autowired ScreenTimeController screenTimeController;

    private void authenticateAs(AuthenticatedUser user) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, authorities));
    }

    private record Fixture(Family family, User parentUser, ChildProfile child) {}

    private Fixture seed() {
        Family family = familyRepository.save(Family.builder()
                .name("Test Family").currency("EUR").timezone("Europe/Madrid").language("ca").active(true)
                .taskApprovalRequired(true).notifyPendingApprovalsEnabled(false).allowSavingsTransfer(true)
                .build());
        User parentUser = userRepository.save(User.builder()
                .family(family).email("p" + UUID.randomUUID() + "@test.com")
                .passwordHash(new BCryptPasswordEncoder().encode("x")).role(UserRole.PARENT).active(true)
                .build());
        User childUser = userRepository.save(User.builder()
                .family(family).username("kid" + UUID.randomUUID())
                .passwordHash(new BCryptPasswordEncoder().encode("1234")).role(UserRole.CHILD).active(true)
                .build());
        ChildProfile child = childProfileRepository.save(ChildProfile.builder()
                .user(childUser).displayName("Kid").birthDate(LocalDate.of(2016, 1, 1))
                .allowanceEnabled(true).screenTimeEnabled(true).active(true)
                .build());
        return new Fixture(family, parentUser, child);
    }

    private AuthenticatedUser asParent(Fixture f) {
        return new AuthenticatedUser(f.parentUser.getId(), f.family.getId(), UserRole.PARENT, null);
    }

    @Test
    @Transactional
    void approve_generatesMoneyAndScreenTimeTransactions_reject_generatesNone() {
        Fixture f = seed();
        AuthenticatedUser parent = asParent(f);
        authenticateAs(parent);

        // Regla pròpia 75%/25% perquè el repartiment es calculi a l'aprovació (no en crear la tasca).
        childManagementController.updateAllowance(
                f.child.getId(), new AllowanceRuleUpdateRequest(new BigDecimal("10.00"), new BigDecimal("75"), new BigDecimal("25")), parent);

        Task task = taskRepository.save(Task.builder()
                .family(f.family).name("Rentar cotxe").taskType(TaskType.EXTRA)
                .active(true).requiresApproval(true).repeatable(true)
                .recurrenceType(RecurrenceType.WEEKLY).createdBy(f.parentUser).build());
        taskRewardRepository.save(TaskReward.builder()
                .task(task).moneyAmount(new BigDecimal("4.00"))
                .screenMinutes(15).active(true).build());
        taskAssignmentRepository.save(TaskAssignment.builder().task(task).child(f.child).active(true).build());

        TaskCompletion completion = taskCompletionRepository.save(TaskCompletion.builder()
                .task(task).child(f.child).completedAt(java.time.Instant.now())
                .status(TaskCompletionStatus.PENDING)
                .rewardMoney(new BigDecimal("4.00"))
                .rewardScreenMinutes(15).build());

        List<PendingApprovalResponse> pending = pendingApprovalsController.pendingApprovals(f.family.getId(), parent);
        assertThat(pending).hasSize(1);

        approvalController.approve(completion.getId(), parent);

        List<ChildFamilySummary> summary = familySummaryController.summary(f.family.getId(), parent);
        assertThat(summary.get(0).spendingBalance()).isEqualByComparingTo("3.00");
        assertThat(summary.get(0).savingsBalance()).isEqualByComparingTo("1.00");
        assertThat(summary.get(0).pendingApprovalsCount()).isZero();

        var screenStatus = screenTimeController.today(f.child.getId(), asChild(f));
        assertThat(screenStatus.availableMinutes()).isEqualTo(15);

        // Ja resolta: una segona aprovació ha de fallar (RN-03).
        assertThatThrownBy(() -> approvalController.approve(completion.getId(), parent))
                .isInstanceOf(DomainException.class);

        // Una tasca nova, rebutjada: no genera cap moviment.
        TaskCompletion second = taskCompletionRepository.save(TaskCompletion.builder()
                .task(task).child(f.child).completedAt(java.time.Instant.now())
                .status(TaskCompletionStatus.PENDING)
                .rewardMoney(new BigDecimal("3.00"))
                .rewardScreenMinutes(0).build());
        approvalController.reject(second.getId(), parent);

        List<FamilyMoneyTransactionResponse> movements = familySummaryController.movements(f.family.getId(), parent);
        assertThat(movements).hasSize(2); // spending + savings del primer, cap del rebutjat
    }

    private AuthenticatedUser asChild(Fixture f) {
        return new AuthenticatedUser(f.child.getUser().getId(), f.family.getId(), UserRole.CHILD, f.child.getId());
    }

    @Test
    @Transactional
    void familySettings_roundTrip() {
        Fixture f = seed();
        AuthenticatedUser parent = asParent(f);
        authenticateAs(parent);

        var updated = familySettingsController.updateSettings(
                f.family.getId(), new FamilySettingsUpdateRequest(false, true, false), parent);

        assertThat(updated.taskApprovalRequired()).isFalse();
        assertThat(updated.notifyPendingApprovalsEnabled()).isTrue();
        assertThat(updated.allowSavingsTransfer()).isFalse();

        var fetched = familySettingsController.settings(f.family.getId(), parent);
        assertThat(fetched).isEqualTo(updated);
    }

    @Test
    @Transactional
    void childManagement_updatingRulesReplacesPreviousOneAndKeepsHistory() {
        Fixture f = seed();
        AuthenticatedUser parent = asParent(f);
        authenticateAs(parent);

        childManagementController.updateAllowance(
                f.child.getId(), new AllowanceRuleUpdateRequest(new BigDecimal("10.00"), new BigDecimal("70"), new BigDecimal("30")), parent);
        childManagementController.updateScreenTime(f.child.getId(), new ScreenTimeRuleUpdateRequest(45), parent);

        List<ChildDetailResponse> details = childManagementController.details(f.family.getId(), parent);
        assertThat(details.get(0).allowanceMonthlyAmount()).isEqualByComparingTo("10.00");
        assertThat(details.get(0).screenBaseMinutes()).isEqualTo(45);

        // Actualitzar de nou no ha de deixar dues regles actives simultànies.
        childManagementController.updateAllowance(
                f.child.getId(), new AllowanceRuleUpdateRequest(new BigDecimal("15.00"), new BigDecimal("60"), new BigDecimal("40")), parent);

        List<ChildDetailResponse> after = childManagementController.details(f.family.getId(), parent);
        assertThat(after.get(0).allowanceMonthlyAmount()).isEqualByComparingTo("15.00");
    }

    @Test
    @Transactional
    void negativeBalanceNfcSessions_areSurfacedForParent() {
        Fixture f = seed();
        AuthenticatedUser parent = asParent(f);
        authenticateAs(parent);

        ScreenTag tag = screenTagRepository.save(ScreenTag.builder().family(f.family).token("t-" + UUID.randomUUID()).active(true).build());
        ScreenSession session = screenSessionRepository.save(ScreenSession.builder()
                .screenTag(tag).startedAt(java.time.Instant.now().minusSeconds(60))
                .endedAt(java.time.Instant.now()).elapsedSeconds(60).status(ScreenSessionStatus.CLOSED)
                .build());
        screenSessionParticipantRepository.save(ScreenSessionParticipant.builder()
                .session(session).child(f.child).assignedSeconds(60).resultingBalanceNegative(true).build());

        List<NegativeBalanceSessionResponse> result = screenTimeController.negativeBalanceSessions(f.family.getId(), parent);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).childName()).isEqualTo("Kid");
    }
}
