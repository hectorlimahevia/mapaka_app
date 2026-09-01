package cat.mapaka;

import cat.mapaka.allowance.AllowanceGenerationController;
import cat.mapaka.allowance.AllowanceRuleUpdateRequest;
import cat.mapaka.allowance.MonthlyAllowanceResponse;
import cat.mapaka.child.*;
import cat.mapaka.common.DomainException;
import cat.mapaka.family.*;
import cat.mapaka.money.FamilyMoneyTransactionResponse;
import cat.mapaka.money.MoneyTransactionRepository;
import cat.mapaka.money.WalletType;
import cat.mapaka.savings.SavingsGoal;
import cat.mapaka.savings.SavingsGoalRepository;
import cat.mapaka.savings.SavingsGoalStatus;
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
    @Autowired MoneyTransactionRepository moneyTransactionRepository;
    @Autowired SavingsGoalRepository savingsGoalRepository;

    @Autowired FamilySummaryController familySummaryController;
    @Autowired PendingApprovalsController pendingApprovalsController;
    @Autowired ApprovalController approvalController;
    @Autowired FamilySettingsController familySettingsController;
    @Autowired ChildManagementController childManagementController;
    @Autowired ScreenTimeController screenTimeController;
    @Autowired AllowanceGenerationController allowanceGenerationController;
    @Autowired TaskController taskController;
    @Autowired TaskManagementController taskManagementController;

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

    private ChildProfile seedSibling(Family family, String name) {
        User siblingUser = userRepository.save(User.builder()
                .family(family).username("kid" + UUID.randomUUID())
                .passwordHash(new BCryptPasswordEncoder().encode("1234")).role(UserRole.CHILD).active(true)
                .build());
        return childProfileRepository.save(ChildProfile.builder()
                .user(siblingUser).displayName(name).birthDate(LocalDate.of(2015, 1, 1))
                .allowanceEnabled(true).screenTimeEnabled(true).active(true)
                .build());
    }

    private AuthenticatedUser asChild(ChildProfile child) {
        return new AuthenticatedUser(child.getUser().getId(), child.getUser().getFamily().getId(), UserRole.CHILD, child.getId());
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
                .recurrenceType(RecurrenceType.WEEKLY).createdBy(f.parentUser)
                .penaltyMoneyAmount(BigDecimal.ZERO).penaltyScreenMinutes(0).build());
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

        List<FamilyMoneyTransactionResponse> movements =
                familySummaryController.movements(f.family.getId(), null, null, null, 0, 50, parent);
        assertThat(movements).hasSize(2); // spending + savings del primer, cap del rebutjat
    }

    @Test
    @Transactional
    void approve_withActiveGoal_splitsInThreeAndAutoCompletesGoal() {
        Fixture f = seed();
        AuthenticatedUser parent = asParent(f);
        authenticateAs(parent);

        // 80% gastar / 20% estalvi, i un objectiu que es queda un altre 20% del "per gastar".
        childManagementController.updateAllowance(
                f.child.getId(), new AllowanceRuleUpdateRequest(new BigDecimal("10.00"), new BigDecimal("80"), new BigDecimal("20")), parent);
        SavingsGoal goal = savingsGoalRepository.save(SavingsGoal.builder()
                .child(f.child).name("Bici").targetAmount(new BigDecimal("2.00"))
                .allocationPercentage(new BigDecimal("20")).status(SavingsGoalStatus.ACTIVE).build());

        Task task = taskRepository.save(Task.builder()
                .family(f.family).name("Passejar el gos").taskType(TaskType.EXTRA)
                .active(true).requiresApproval(true).repeatable(true)
                .recurrenceType(RecurrenceType.WEEKLY).createdBy(f.parentUser)
                .penaltyMoneyAmount(BigDecimal.ZERO).penaltyScreenMinutes(0).build());
        taskRewardRepository.save(TaskReward.builder()
                .task(task).moneyAmount(new BigDecimal("10.00")).screenMinutes(0).active(true).build());
        taskAssignmentRepository.save(TaskAssignment.builder().task(task).child(f.child).active(true).build());

        TaskCompletion completion = taskCompletionRepository.save(TaskCompletion.builder()
                .task(task).child(f.child).completedAt(java.time.Instant.now())
                .status(TaskCompletionStatus.PENDING)
                .rewardMoney(new BigDecimal("10.00")).rewardScreenMinutes(0).build());

        approvalController.approve(completion.getId(), parent);

        // 10€: 60% gastar (80-20 de l'objectiu) = 6€, 20% estalvi = 2€, 20% objectiu = 2€.
        List<ChildFamilySummary> summary = familySummaryController.summary(f.family.getId(), parent);
        assertThat(summary.get(0).spendingBalance()).isEqualByComparingTo("6.00");
        assertThat(summary.get(0).savingsBalance()).isEqualByComparingTo("2.00");
        assertThat(moneyTransactionRepository.balanceFor(f.child.getId(), WalletType.GOAL)).isEqualByComparingTo("2.00");

        // L'objectiu (target 2.00) s'ha completat sol amb aquesta única aportació.
        SavingsGoal reloaded = savingsGoalRepository.findById(goal.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SavingsGoalStatus.COMPLETED);

        // Un cop completat, el seu 20% torna a "gastar" en el següent repartiment.
        TaskCompletion second = taskCompletionRepository.save(TaskCompletion.builder()
                .task(task).child(f.child).completedAt(java.time.Instant.now())
                .status(TaskCompletionStatus.PENDING)
                .rewardMoney(new BigDecimal("10.00")).rewardScreenMinutes(0).build());
        approvalController.approve(second.getId(), parent);

        List<ChildFamilySummary> summaryAfter = familySummaryController.summary(f.family.getId(), parent);
        assertThat(summaryAfter.get(0).spendingBalance()).isEqualByComparingTo("14.00"); // 6 + 8 (80% de 10)
        assertThat(summaryAfter.get(0).savingsBalance()).isEqualByComparingTo("4.00"); // 2 + 2 (20% de 10)
        assertThat(moneyTransactionRepository.balanceFor(f.child.getId(), WalletType.GOAL)).isEqualByComparingTo("2.00"); // sense canvis
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

    @Test
    @Transactional
    void extraTask_collaborationBlocksOthersAndSplitsRewardEvenlyOnGroupApproval() {
        Fixture f = seed();
        AuthenticatedUser parent = asParent(f);
        authenticateAs(parent);
        ChildProfile sibling = seedSibling(f.family, "Sibling");
        ChildProfile outsider = seedSibling(f.family, "Outsider");

        Task task = taskRepository.save(Task.builder()
                .family(f.family).name("Rentar el cotxe").taskType(TaskType.EXTRA)
                .active(true).requiresApproval(true).repeatable(true)
                .recurrenceType(RecurrenceType.WEEKLY).createdBy(f.parentUser)
                .penaltyMoneyAmount(BigDecimal.ZERO).penaltyScreenMinutes(0).build());
        taskRewardRepository.save(TaskReward.builder()
                .task(task).moneyAmount(new BigDecimal("10.00")).screenMinutes(20).active(true).build());

        // f.child la reclama amb l'ajuda de sibling.
        authenticateAs(asChild(f.child));
        taskController.complete(task.getId(), new cat.mapaka.task.CompleteTaskRequest(List.of(sibling.getId())), asChild(f.child));

        // outsider, que no hi participa, la veu com a ja reclamada — i no la pot marcar.
        List<ChildTaskResponse> outsiderView = taskController.tasks(outsider.getId(), asChild(outsider));
        assertThat(outsiderView.get(0).status()).isEqualTo(ChildTaskStatus.CLAIMED_BY_OTHERS);
        assertThatThrownBy(() -> taskController.complete(
                task.getId(), new cat.mapaka.task.CompleteTaskRequest(List.of()), asChild(outsider)))
                .isInstanceOf(DomainException.class);

        List<ChildTaskResponse> siblingView = taskController.tasks(sibling.getId(), asChild(sibling));
        assertThat(siblingView.get(0).status()).isEqualTo(ChildTaskStatus.PENDING);
        assertThat(siblingView.get(0).participantNames()).containsExactlyInAnyOrder("Kid", "Sibling");

        authenticateAs(parent);
        List<PendingApprovalResponse> pending = pendingApprovalsController.pendingApprovals(f.family.getId(), parent);
        assertThat(pending).hasSize(2);
        UUID groupId = pending.get(0).completionGroupId();
        assertThat(pending).allMatch(p -> p.completionGroupId().equals(groupId));

        approvalController.approveGroup(groupId, parent);

        // 10€ i 20 min repartits a parts iguals entre els dos participants (5€ i 10 min cadascun).
        List<ChildFamilySummary> summary = familySummaryController.summary(f.family.getId(), parent);
        summary.forEach(s -> {
            if (s.childId().equals(f.child.getId()) || s.childId().equals(sibling.getId())) {
                assertThat(s.spendingBalance().add(s.savingsBalance())).isEqualByComparingTo("5.00");
            }
        });
    }

    @Test
    @Transactional
    void responsibilityTask_incompleteListsChildUntilPenaltyApplied() {
        Fixture f = seed();
        AuthenticatedUser parent = asParent(f);
        authenticateAs(parent);

        Task task = taskRepository.save(Task.builder()
                .family(f.family).name("Fer el llit").taskType(TaskType.RESPONSIBILITY)
                .active(true).requiresApproval(true).repeatable(true)
                .recurrenceType(RecurrenceType.DAILY).createdBy(f.parentUser)
                .penaltyMoneyAmount(new BigDecimal("1.00")).penaltyScreenMinutes(10).build());
        taskRewardRepository.save(TaskReward.builder()
                .task(task).moneyAmount(new BigDecimal("0.50")).screenMinutes(0).active(true).build());
        taskAssignmentRepository.save(TaskAssignment.builder().task(task).child(f.child).active(true).build());

        List<IncompleteTaskResponse> incomplete = taskManagementController.incomplete(null, parent);
        assertThat(incomplete).hasSize(1);
        assertThat(incomplete.get(0).childId()).isEqualTo(f.child.getId());

        taskManagementController.applyPenalty(task.getId(), f.child.getId(), parent);

        List<ChildFamilySummary> summary = familySummaryController.summary(f.family.getId(), parent);
        // -1.00€ repartit gastar/estalvi segons el percentatge per defecte (100% gastar, sense regla).
        assertThat(summary.get(0).spendingBalance().add(summary.get(0).savingsBalance())).isEqualByComparingTo("-1.00");
    }

    @Test
    @Transactional
    void responsibilityTask_requiresPenalty_rewardIsOptional_extraRequiresReward() {
        // Feedback real: una Responsabilitat sense penalització no té cap conseqüència real
        // de no fer-la, per això la penalització hi és sempre obligatòria (la recompensa no);
        // combinacions vàlides: recompensa+penalització, o només penalització. Una Extra, en
        // canvi, no té penalització i per tant sempre necessita recompensa.
        Fixture f = seed();
        AuthenticatedUser parent = asParent(f);
        authenticateAs(parent);

        // Responsabilitat amb recompensa i SENSE penalització -> rebutjada.
        assertThatThrownBy(() -> taskManagementController.create(
                new CreateTaskRequest("Fer el llit", null, TaskType.RESPONSIBILITY, null, true,
                        RecurrenceType.DAILY, new BigDecimal("0.50"), 0, BigDecimal.ZERO, 0,
                        List.of(f.child.getId())),
                parent))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).code()).isEqualTo("INVALID_TASK_PENALTY"));

        // Responsabilitat SENSE recompensa i amb penalització -> acceptada.
        TaskManagementResponse onlyPenalty = taskManagementController.create(
                new CreateTaskRequest("Recollir joguines", null, TaskType.RESPONSIBILITY, null, true,
                        RecurrenceType.DAILY, BigDecimal.ZERO, 0, new BigDecimal("1.00"), 5,
                        List.of(f.child.getId())),
                parent).getBody();
        assertThat(onlyPenalty).isNotNull();
        assertThat(onlyPenalty.rewardMoney()).isEqualByComparingTo("0.00");
        assertThat(onlyPenalty.penaltyMoneyAmount()).isEqualByComparingTo("1.00");

        // Responsabilitat amb recompensa I penalització -> acceptada.
        TaskManagementResponse both = taskManagementController.create(
                new CreateTaskRequest("Treure les escombraries", null, TaskType.RESPONSIBILITY, null, true,
                        RecurrenceType.DAILY, new BigDecimal("0.50"), 0, new BigDecimal("1.00"), 0,
                        List.of(f.child.getId())),
                parent).getBody();
        assertThat(both).isNotNull();

        // Extra SENSE recompensa -> rebutjada (mai té penalització que la compensi).
        assertThatThrownBy(() -> taskManagementController.create(
                new CreateTaskRequest("Ajudar amb la compra", null, TaskType.EXTRA, null, true,
                        RecurrenceType.NONE, BigDecimal.ZERO, 0, null, null, List.of()),
                parent))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).code()).isEqualTo("INVALID_TASK_REWARD"));
    }

    @Test
    @Transactional
    void confirmAllowance_creditsMoneyAndScreenTimeInTheSameAct() {
        // Prompt 16 (punt 14/24): abans el temps de pantalla es generava per separat i
        // diàriament; ara "Generar la paga del mes" ha de crear els dos moviments alhora,
        // en confirmar, i no abans.
        Fixture f = seed();
        AuthenticatedUser parent = asParent(f);
        authenticateAs(parent);

        childManagementController.updateAllowance(
                f.child.getId(), new AllowanceRuleUpdateRequest(new BigDecimal("10.00"), new BigDecimal("70"), new BigDecimal("30")), parent);
        childManagementController.updateScreenTime(f.child.getId(), new ScreenTimeRuleUpdateRequest(240), parent);

        List<MonthlyAllowanceResponse> drafts = allowanceGenerationController.generate(parent);
        assertThat(drafts).hasSize(1);

        // Generar només crea el DRAFT — cap moviment encara, ni de diners ni de pantalla.
        List<ChildFamilySummary> beforeConfirm = familySummaryController.summary(f.family.getId(), parent);
        assertThat(beforeConfirm.get(0).totalBalance()).isEqualByComparingTo("0.00");
        assertThat(screenTimeController.today(f.child.getId(), asChild(f.child)).availableMinutes()).isZero();

        allowanceGenerationController.confirm(drafts.get(0).id(), parent);

        List<ChildFamilySummary> afterConfirm = familySummaryController.summary(f.family.getId(), parent);
        assertThat(afterConfirm.get(0).spendingBalance()).isEqualByComparingTo("7.00");
        assertThat(afterConfirm.get(0).savingsBalance()).isEqualByComparingTo("3.00");
        assertThat(screenTimeController.today(f.child.getId(), asChild(f.child)).availableMinutes()).isEqualTo(240);

        // Confirmar-la un segon cop ha de fallar (RN-03, ja no és DRAFT) — i no duplica res.
        assertThatThrownBy(() -> allowanceGenerationController.confirm(drafts.get(0).id(), parent))
                .isInstanceOf(DomainException.class);
        assertThat(screenTimeController.today(f.child.getId(), asChild(f.child)).availableMinutes()).isEqualTo(240);
    }

    @Test
    @Transactional
    void pendingAllowances_surfacesDraftsFromAnEarlierSessionAndClearsOnceConfirmed() {
        // Bug real: comparar l'enum natiu de Postgres (status) amb un literal a la JPQL
        // ("a.status = ...AllowanceStatus.DRAFT") generava un cast SQL amb el nom de la
        // classe Java (::AllowanceStatus) en lloc del tipus real de Postgres
        // (allowance_status), i Postgres el rebutjava — verificat contra Postgres real, no
        // una base de dades en memòria, perquè és precisament aquí on fallava.
        Fixture f = seed();
        AuthenticatedUser parent = asParent(f);
        authenticateAs(parent);

        childManagementController.updateAllowance(
                f.child.getId(), new AllowanceRuleUpdateRequest(new BigDecimal("10.00"), new BigDecimal("70"), new BigDecimal("30")), parent);

        assertThat(allowanceGenerationController.pending(parent)).isEmpty();

        List<MonthlyAllowanceResponse> drafts = allowanceGenerationController.generate(parent);
        assertThat(drafts).hasSize(1);

        // Simula una sessió nova (app tancada/reinstal·lada abans de confirmar): el DRAFT
        // ha de seguir sent recuperable encara que ja no hi hagi la resposta original de
        // generate() en memòria.
        assertThat(allowanceGenerationController.pending(parent)).hasSize(1);

        allowanceGenerationController.confirm(drafts.get(0).id(), parent);

        assertThat(allowanceGenerationController.pending(parent)).isEmpty();
    }

    @Test
    @Transactional
    void deactivateChild_hidesFromFamilySummary_preservesHistory_andReactivateBringsBack() {
        Fixture f = seed();
        AuthenticatedUser parent = asParent(f);
        authenticateAs(parent);

        childManagementController.updateAllowance(
                f.child.getId(), new AllowanceRuleUpdateRequest(new BigDecimal("10.00"), new BigDecimal("70"), new BigDecimal("30")), parent);
        List<MonthlyAllowanceResponse> drafts = allowanceGenerationController.generate(parent);
        allowanceGenerationController.confirm(drafts.get(0).id(), parent);

        assertThat(familySummaryController.summary(f.family.getId(), parent)).hasSize(1);

        childManagementController.deactivate(f.child.getId(), parent);
        assertThat(familySummaryController.summary(f.family.getId(), parent)).isEmpty();

        // Desactivar no esborra ni un cèntim de l'històric — el fill només queda fora
        // de l'ús actiu, mai perd el que ja s'havia guardat.
        assertThat(moneyTransactionRepository.balanceFor(f.child.getId(), cat.mapaka.money.WalletType.SPENDING))
                .isEqualByComparingTo("7.00");

        childManagementController.reactivate(f.child.getId(), parent);
        assertThat(familySummaryController.summary(f.family.getId(), parent)).hasSize(1);
    }

    @Test
    @Transactional
    void deleteChild_succeedsWhenNeverUsed_butRefusesOnceThereIsHistory() {
        Fixture f = seed();
        AuthenticatedUser parent = asParent(f);
        authenticateAs(parent);

        // Un fill que mai ha tingut cap activitat real es pot esborrar de debò — la
        // configuració (regla de paga) no compta com a "activitat", és configuració.
        childManagementController.updateAllowance(
                f.child.getId(), new AllowanceRuleUpdateRequest(new BigDecimal("10.00"), new BigDecimal("70"), new BigDecimal("30")), parent);
        childManagementController.delete(f.child.getId(), parent);
        assertThat(childManagementController.details(f.family.getId(), parent)).isEmpty();

        // Un fill amb activitat real (paga confirmada) mai es pot esborrar de debò.
        ChildProfile other = seedSibling(f.family, "HasHistory");
        childManagementController.updateAllowance(
                other.getId(), new AllowanceRuleUpdateRequest(new BigDecimal("10.00"), new BigDecimal("70"), new BigDecimal("30")), parent);
        List<MonthlyAllowanceResponse> drafts = allowanceGenerationController.generate(parent);
        allowanceGenerationController.confirm(drafts.get(0).id(), parent);

        assertThatThrownBy(() -> childManagementController.delete(other.getId(), parent))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "CHILD_HAS_HISTORY");
        assertThat(childManagementController.details(f.family.getId(), parent)).hasSize(1);
    }
}
