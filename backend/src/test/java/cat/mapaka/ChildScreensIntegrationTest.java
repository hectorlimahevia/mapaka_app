package cat.mapaka;

import cat.mapaka.child.ChildProfile;
import cat.mapaka.child.ChildProfileRepository;
import cat.mapaka.common.DomainException;
import cat.mapaka.common.TransactionType;
import cat.mapaka.family.Family;
import cat.mapaka.family.FamilyRepository;
import cat.mapaka.money.*;
import cat.mapaka.savings.CreateSavingsGoalRequest;
import cat.mapaka.savings.SavingsGoal;
import cat.mapaka.savings.SavingsGoalController;
import cat.mapaka.savings.SavingsGoalInvitationResponse;
import cat.mapaka.savings.SavingsGoalRepository;
import cat.mapaka.savings.SavingsGoalResponse;
import cat.mapaka.savings.SavingsGoalStatus;
import cat.mapaka.screentime.ScreenTimeController;
import cat.mapaka.screentime.ScreenTimeRule;
import cat.mapaka.screentime.ScreenTimeRuleRepository;
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
class ChildScreensIntegrationTest {

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
    @Autowired MoneyTransactionRepository moneyTransactionRepository;
    @Autowired SavingsGoalRepository savingsGoalRepository;
    @Autowired ScreenTimeRuleRepository screenTimeRuleRepository;
    @Autowired TaskRepository taskRepository;
    @Autowired TaskRewardRepository taskRewardRepository;
    @Autowired TaskAssignmentRepository taskAssignmentRepository;
    @Autowired TaskCompletionRepository taskCompletionRepository;

    @Autowired MoneyController moneyController;
    @Autowired SavingsGoalController savingsGoalController;
    @Autowired ScreenTimeController screenTimeController;
    @Autowired TaskController taskController;

    private ChildProfile seedChild() {
        Family family = familyRepository.save(Family.builder()
                .name("Test Family").currency("EUR").timezone("Europe/Madrid").language("ca").active(true)
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
        moneyTransactionRepository.save(MoneyTransaction.builder()
                .child(child).walletType(WalletType.SPENDING).transactionType(TransactionType.CREDIT)
                .amount(new BigDecimal("14.00")).sourceType(MoneySourceType.MONTHLY_ALLOWANCE)
                .createdBy(parentUser).build());
        moneyTransactionRepository.save(MoneyTransaction.builder()
                .child(child).walletType(WalletType.SPENDING).transactionType(TransactionType.DEBIT)
                .amount(new BigDecimal("5.00")).sourceType(MoneySourceType.PURCHASE)
                .createdBy(parentUser).build());
        moneyTransactionRepository.save(MoneyTransaction.builder()
                .child(child).walletType(WalletType.SAVINGS).transactionType(TransactionType.CREDIT)
                .amount(new BigDecimal("6.00")).sourceType(MoneySourceType.MONTHLY_ALLOWANCE)
                .createdBy(parentUser).build());
        return child;
    }

    private ChildProfile seedSibling(ChildProfile existingChild, String displayName) {
        Family family = existingChild.getUser().getFamily();
        User siblingUser = userRepository.save(User.builder()
                .family(family).username("kid" + UUID.randomUUID())
                .passwordHash(new BCryptPasswordEncoder().encode("1234")).role(UserRole.CHILD).active(true)
                .build());
        return childProfileRepository.save(ChildProfile.builder()
                .user(siblingUser).displayName(displayName).birthDate(LocalDate.of(2017, 1, 1))
                .allowanceEnabled(true).screenTimeEnabled(true).active(true)
                .build());
    }

    private AuthenticatedUser asChild(ChildProfile child) {
        return new AuthenticatedUser(child.getUser().getId(), child.getUser().getFamily().getId(), UserRole.CHILD, child.getId());
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
    void wallet_sumsLedgerCorrectly() {
        ChildProfile child = seedChild();
        WalletResponse wallet = moneyController.wallet(child.getId(), asChild(child));
        assertThat(wallet.spendingBalance()).isEqualByComparingTo("9.00");
        assertThat(wallet.savingsBalance()).isEqualByComparingTo("6.00");
        assertThat(wallet.total()).isEqualByComparingTo("15.00");
        assertThat(moneyController.transactions(child.getId(), asChild(child))).hasSize(3);
    }

    @Test
    @Transactional
    void savingsGoal_progressIsOwnGoalWalletNotSharedSavings() {
        ChildProfile child = seedChild();
        cat.mapaka.savings.SavingsGoal goal = savingsGoalRepository.save(cat.mapaka.savings.SavingsGoal.builder()
                .child(child).name("Bici").targetAmount(new BigDecimal("120.00"))
                .allocationPercentage(BigDecimal.ZERO).status(SavingsGoalStatus.ACTIVE)
                .build());
        moneyTransactionRepository.save(MoneyTransaction.builder()
                .child(child).walletType(WalletType.GOAL).transactionType(TransactionType.CREDIT)
                .amount(new BigDecimal("4.00")).sourceType(MoneySourceType.GOAL_CONTRIBUTION).sourceId(goal.getId())
                .createdBy(child.getUser()).build());

        List<cat.mapaka.savings.SavingsGoalResponse> goals = savingsGoalController.goals(child.getId(), asChild(child));
        assertThat(goals).hasSize(1);
        // 4.00 (nomes les aportacions pròpies de l'objectiu), no els 6.00 d'estalvi general del fill.
        assertThat(goals.get(0).currentAmount()).isEqualByComparingTo("4.00");
    }

    @Test
    @Transactional
    void sharedGoal_inviteSiblingThenAccept_clonesConditionsIntoOwnGoal_rejectCreatesNothing() {
        ChildProfile inviter = seedChild();
        ChildProfile accepter = seedSibling(inviter, "Accepter");
        ChildProfile rejecter = seedSibling(inviter, "Rejecter");

        SavingsGoalResponse created = savingsGoalController.create(
                inviter.getId(),
                new CreateSavingsGoalRequest("Bici", new BigDecimal("100.00"), BigDecimal.ZERO,
                        List.of(accepter.getId(), rejecter.getId())),
                asChild(inviter)).getBody();
        assertThat(created).isNotNull();

        List<SavingsGoalInvitationResponse> accepterInvitations =
                savingsGoalController.pendingInvitations(accepter.getId(), asChild(accepter));
        assertThat(accepterInvitations).hasSize(1);
        assertThat(accepterInvitations.get(0).goalName()).isEqualTo("Bici");
        assertThat(accepterInvitations.get(0).targetAmount()).isEqualByComparingTo("100.00");
        assertThat(accepterInvitations.get(0).inviterChildName()).isEqualTo("Kid");

        SavingsGoalResponse accepterGoal = savingsGoalController.acceptInvitation(
                accepter.getId(), accepterInvitations.get(0).id(), asChild(accepter));
        assertThat(accepterGoal.name()).isEqualTo("Bici");
        assertThat(accepterGoal.targetAmount()).isEqualByComparingTo("100.00");
        assertThat(savingsGoalController.goals(accepter.getId(), asChild(accepter))).hasSize(1);
        assertThat(savingsGoalController.pendingInvitations(accepter.getId(), asChild(accepter))).isEmpty();

        List<SavingsGoalInvitationResponse> rejecterInvitations =
                savingsGoalController.pendingInvitations(rejecter.getId(), asChild(rejecter));
        assertThat(rejecterInvitations).hasSize(1);
        savingsGoalController.rejectInvitation(rejecter.getId(), rejecterInvitations.get(0).id(), asChild(rejecter));
        assertThat(savingsGoalController.goals(rejecter.getId(), asChild(rejecter))).isEmpty();
        assertThat(savingsGoalController.pendingInvitations(rejecter.getId(), asChild(rejecter))).isEmpty();
    }

    @Test
    @Transactional
    void screenTime_todayStatusIsReadOnly_neverCreditsByItself() {
        // Prompt 16 (punt 14/24): el temps de pantalla ja no es genera en consultar l'estat
        // ("baseMinutes" és només informatiu, per pintar l'anell) — l'únic que crea moviments
        // reals és AllowanceGenerationService.confirm(), en el mateix acte que la paga.
        ChildProfile child = seedChild();
        screenTimeRuleRepository.save(ScreenTimeRule.builder()
                .child(child).baseMinutes(45).rolloverEnabled(false)
                .validFrom(LocalDate.now().minusDays(1)).active(true)
                .build());
        AuthenticatedUser principal = asChild(child);
        UUID childId = child.getId();

        var first = screenTimeController.today(childId, principal);
        assertThat(first.baseMinutes()).isEqualTo(45);
        assertThat(first.availableMinutes()).isEqualTo(0); // res acreditat encara

        var second = screenTimeController.today(childId, principal);
        assertThat(second.availableMinutes()).isEqualTo(0); // consultar-ho no crea cap moviment
    }

    @Test
    @Transactional
    void task_childCanCompleteOnce_thenMustWaitForApprovalBeforeRetrying() {
        ChildProfile child = seedChild();
        Family family = child.getUser().getFamily();

        Task task = taskRepository.save(Task.builder()
                .family(family).name("Rentar cotxe").taskType(TaskType.RESPONSIBILITY)
                .active(true).requiresApproval(true).repeatable(true)
                .recurrenceType(RecurrenceType.WEEKLY).createdBy(child.getUser())
                .penaltyMoneyAmount(BigDecimal.ZERO).penaltyScreenMinutes(0)
                .build());
        taskRewardRepository.save(TaskReward.builder()
                .task(task).moneyAmount(new BigDecimal("3.00"))
                .screenMinutes(15).active(true).build());
        taskAssignmentRepository.save(TaskAssignment.builder().task(task).child(child).active(true).build());

        List<ChildTaskResponse> before = taskController.tasks(child.getId(), asChild(child));
        assertThat(before.get(0).status()).isEqualTo(ChildTaskStatus.AVAILABLE);

        authenticateAs(asChild(child));
        taskController.complete(task.getId(), new cat.mapaka.task.CompleteTaskRequest(List.of()), asChild(child));

        List<ChildTaskResponse> after = taskController.tasks(child.getId(), asChild(child));
        assertThat(after.get(0).status()).isEqualTo(ChildTaskStatus.PENDING);

        try {
            assertThatThrownBy(() -> taskController.complete(task.getId(), new cat.mapaka.task.CompleteTaskRequest(List.of()), asChild(child)))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("període");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
