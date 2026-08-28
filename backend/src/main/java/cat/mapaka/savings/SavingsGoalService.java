package cat.mapaka.savings;

import cat.mapaka.allowance.AllowanceRuleService;
import cat.mapaka.child.ChildAccessService;
import cat.mapaka.child.ChildProfile;
import cat.mapaka.common.DomainException;
import cat.mapaka.common.TransactionType;
import cat.mapaka.money.MoneySourceType;
import cat.mapaka.money.MoneyTransaction;
import cat.mapaka.money.MoneyTransactionRepository;
import cat.mapaka.money.WalletType;
import cat.mapaka.security.AuthenticatedUser;
import cat.mapaka.user.User;
import cat.mapaka.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Objectius d'estalvi amb percentatge propi (Prompt 15) — la suma dels `allocationPercentage`
 * de tots els objectius ACTIVE d'un fill mai pot superar el seu percentatge de gastar vigent,
 * perquè MoneySplitCalculator no acabi restant més del que hi ha. */
@Service
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final MoneyTransactionRepository moneyTransactionRepository;
    private final AllowanceRuleService allowanceRuleService;
    private final ChildAccessService childAccessService;
    private final DonationRepository donationRepository;
    private final UserRepository userRepository;

    public SavingsGoalService(
            SavingsGoalRepository savingsGoalRepository,
            MoneyTransactionRepository moneyTransactionRepository,
            AllowanceRuleService allowanceRuleService,
            ChildAccessService childAccessService,
            DonationRepository donationRepository,
            UserRepository userRepository) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.moneyTransactionRepository = moneyTransactionRepository;
        this.allowanceRuleService = allowanceRuleService;
        this.childAccessService = childAccessService;
        this.donationRepository = donationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<SavingsGoalResponse> list(UUID childId) {
        return savingsGoalRepository.findByChildIdAndStatusNot(childId, SavingsGoalStatus.CANCELLED).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SavingsGoalResponse create(ChildProfile child, CreateSavingsGoalRequest request) {
        BigDecimal allocation = allocationOrZero(request);
        requireAllocationFits(child, allocation, null);
        SavingsGoal goal = savingsGoalRepository.save(SavingsGoal.builder()
                .child(child)
                .name(request.name())
                .targetAmount(request.targetAmount())
                .allocationPercentage(allocation)
                .status(SavingsGoalStatus.ACTIVE)
                .build());
        return toResponse(goal);
    }

    @Transactional
    public SavingsGoalResponse update(SavingsGoal goal, CreateSavingsGoalRequest request) {
        BigDecimal allocation = allocationOrZero(request);
        requireAllocationFits(goal.getChild(), allocation, goal.getId());
        goal.setName(request.name());
        goal.setTargetAmount(request.targetAmount());
        goal.setAllocationPercentage(allocation);
        savingsGoalRepository.save(goal);
        return toResponse(goal);
    }

    public SavingsGoal requireOwnedBy(UUID goalId, UUID childId) {
        SavingsGoal goal = savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new DomainException("SAVINGS_GOAL_NOT_FOUND", HttpStatus.NOT_FOUND, "Objectiu no trobat"));
        if (!goal.getChild().getId().equals(childId)) {
            throw new DomainException("ACCESS_DENIED", HttpStatus.FORBIDDEN, "Aquest objectiu no és d'aquest fill");
        }
        return goal;
    }

    /** Donació d'un tercer cap a un objectiu (Prompt 15, secció 8.2) — només PARENT
     * (comprovat pel `@PreAuthorize` del controller). Es guarda com a `Donation` (traçabilitat
     * de qui ha donat i per què) i, per separat, com a `MoneyTransaction` de la wallet GOAL
     * perquè sumi al progrés — mai passa per `MoneySplitCalculator`, ni resta ni reparteix
     * res del gastar/estalvi del fill. */
    @Transactional
    public SavingsGoalResponse donate(UUID goalId, CreateDonationRequest request, AuthenticatedUser requester) {
        SavingsGoal goal = savingsGoalRepository.findById(goalId)
                .orElseThrow(() -> new DomainException("SAVINGS_GOAL_NOT_FOUND", HttpStatus.NOT_FOUND, "Objectiu no trobat"));
        ChildProfile child = childAccessService.requireAccess(goal.getChild().getId(), requester);
        User actor = userRepository.getReferenceById(requester.userId());

        donationRepository.save(Donation.builder()
                .savingsGoal(goal)
                .family(child.getUser().getFamily())
                .donorName(request.donorName())
                .message(request.message())
                .amount(request.amount())
                .createdByUser(actor)
                .build());

        moneyTransactionRepository.save(MoneyTransaction.builder()
                .child(child).walletType(WalletType.GOAL).transactionType(TransactionType.CREDIT)
                .amount(request.amount()).description(request.donorName())
                .sourceType(MoneySourceType.DONATION).sourceId(goal.getId())
                .createdBy(actor).build());

        checkCompletion(goal);
        return toResponse(goal);
    }

    /** El propi fill mou un import puntual del seu "per gastar" cap a un objectiu (ajust
     * posterior) — a diferència d'una donació, aquí SÍ que surt del seu propi saldo, per
     * això mai pot superar el que té disponible a la wallet SPENDING en aquell moment.
     * Es guarden dues files (DEBIT SPENDING + CREDIT GOAL) lligades pel mateix
     * `transferReferenceId`, mai passa per `MoneySplitCalculator` perquè no és un
     * repartiment nou, és un moviment entre dues wallets que el fill ja té. */
    @Transactional
    public SavingsGoalResponse contribute(UUID childId, UUID goalId, BigDecimal amount, AuthenticatedUser requester) {
        ChildProfile child = childAccessService.requireAccess(childId, requester);
        SavingsGoal goal = requireOwnedBy(goalId, childId);

        BigDecimal spendingBalance = moneyTransactionRepository.balanceFor(childId, WalletType.SPENDING);
        if (amount.compareTo(spendingBalance) > 0) {
            throw new DomainException("CONTRIBUTION_EXCEEDS_SPENDING_BALANCE", HttpStatus.BAD_REQUEST,
                    "L'import supera el que tens disponible per gastar");
        }

        User actor = userRepository.getReferenceById(requester.userId());
        UUID transferReferenceId = UUID.randomUUID();

        moneyTransactionRepository.save(MoneyTransaction.builder()
                .child(child).walletType(WalletType.SPENDING).transactionType(TransactionType.DEBIT)
                .amount(amount).description(goal.getName())
                .sourceType(MoneySourceType.GOAL_CONTRIBUTION).sourceId(goal.getId())
                .transferReferenceId(transferReferenceId).createdBy(actor).build());
        moneyTransactionRepository.save(MoneyTransaction.builder()
                .child(child).walletType(WalletType.GOAL).transactionType(TransactionType.CREDIT)
                .amount(amount).description(goal.getName())
                .sourceType(MoneySourceType.GOAL_CONTRIBUTION).sourceId(goal.getId())
                .transferReferenceId(transferReferenceId).createdBy(actor).build());

        checkCompletion(goal);
        return toResponse(goal);
    }

    /** Eliminar un objectiu (ajust posterior) — mai fa desaparèixer diner real: si l'objectiu
     * ja tenia progrés (repartiment automàtic, aportacions o donacions), es retorna a "per
     * gastar" abans de res, amb el mateix parell DEBIT GOAL + CREDIT SPENDING que fa servir
     * `contribute` (en sentit invers). L'objectiu no s'esborra mai de la taula: es marca
     * `CANCELLED` (valor de l'enum que ja existia des de la Fase A però que cap flux feia
     * servir), perquè les seves `MoneyTransaction` d'origen mantinguin sentit i l'objectiu
     * desaparegui tant de la llista activa com de l'historial. Idempotent si ja estava cancel·lat.
     */
    @Transactional
    public void delete(UUID childId, UUID goalId, AuthenticatedUser requester) {
        ChildProfile child = childAccessService.requireAccess(childId, requester);
        SavingsGoal goal = requireOwnedBy(goalId, childId);
        if (goal.getStatus() == SavingsGoalStatus.CANCELLED) {
            return;
        }

        BigDecimal progress = moneyTransactionRepository.goalProgress(goal.getId());
        if (progress.compareTo(BigDecimal.ZERO) > 0) {
            User actor = userRepository.getReferenceById(requester.userId());
            UUID transferReferenceId = UUID.randomUUID();
            moneyTransactionRepository.save(MoneyTransaction.builder()
                    .child(child).walletType(WalletType.GOAL).transactionType(TransactionType.DEBIT)
                    .amount(progress).description(goal.getName())
                    .sourceType(MoneySourceType.GOAL_CONTRIBUTION).sourceId(goal.getId())
                    .transferReferenceId(transferReferenceId).createdBy(actor).build());
            moneyTransactionRepository.save(MoneyTransaction.builder()
                    .child(child).walletType(WalletType.SPENDING).transactionType(TransactionType.CREDIT)
                    .amount(progress).description(goal.getName())
                    .sourceType(MoneySourceType.GOAL_CONTRIBUTION).sourceId(goal.getId())
                    .transferReferenceId(transferReferenceId).createdBy(actor).build());
        }

        goal.setStatus(SavingsGoalStatus.CANCELLED);
        savingsGoalRepository.save(goal);
    }

    private void checkCompletion(SavingsGoal goal) {
        if (goal.getStatus() != SavingsGoalStatus.ACTIVE) {
            return;
        }
        BigDecimal progress = moneyTransactionRepository.goalProgress(goal.getId());
        if (progress.compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(SavingsGoalStatus.COMPLETED);
            goal.setCompletedAt(Instant.now());
            savingsGoalRepository.save(goal);
        }
    }

    private BigDecimal allocationOrZero(CreateSavingsGoalRequest request) {
        return request.allocationPercentage() != null ? request.allocationPercentage() : BigDecimal.ZERO;
    }

    private void requireAllocationFits(ChildProfile child, BigDecimal newAllocation, UUID excludingGoalId) {
        BigDecimal spendingPercentage = allowanceRuleService.resolveSpendingPercentage(child);
        BigDecimal othersSum = savingsGoalRepository.findByChildIdAndStatus(child.getId(), SavingsGoalStatus.ACTIVE).stream()
                .filter(g -> excludingGoalId == null || !g.getId().equals(excludingGoalId))
                .map(SavingsGoal::getAllocationPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (othersSum.add(newAllocation).compareTo(spendingPercentage) > 0) {
            throw new DomainException("GOAL_PERCENTAGE_EXCEEDS_AVAILABLE", HttpStatus.BAD_REQUEST,
                    "El percentatge de l'objectiu supera el marge disponible de \"per gastar\"");
        }
    }

    private SavingsGoalResponse toResponse(SavingsGoal goal) {
        BigDecimal currentAmount = moneyTransactionRepository.goalProgress(goal.getId());
        return SavingsGoalResponse.from(goal, currentAmount);
    }
}
