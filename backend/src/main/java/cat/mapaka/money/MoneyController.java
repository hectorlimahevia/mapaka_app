package cat.mapaka.money;

import cat.mapaka.child.ChildAccessService;
import cat.mapaka.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class MoneyController {

    private final ChildAccessService childAccessService;
    private final MoneyTransactionRepository moneyTransactionRepository;

    public MoneyController(ChildAccessService childAccessService, MoneyTransactionRepository moneyTransactionRepository) {
        this.childAccessService = childAccessService;
        this.moneyTransactionRepository = moneyTransactionRepository;
    }

    @GetMapping("/api/children/{id}/wallet")
    public WalletResponse wallet(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        childAccessService.requireAccess(id, user);
        var spending = moneyTransactionRepository.balanceFor(id, WalletType.SPENDING);
        var savings = moneyTransactionRepository.balanceFor(id, WalletType.SAVINGS);
        return new WalletResponse(spending, savings, spending.add(savings));
    }

    @GetMapping("/api/children/{id}/money-transactions")
    public List<MoneyTransactionResponse> transactions(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        childAccessService.requireAccess(id, user);
        return moneyTransactionRepository.findByChildIdOrderByCreatedAtDesc(id).stream()
                .map(MoneyTransactionResponse::from)
                .toList();
    }
}
