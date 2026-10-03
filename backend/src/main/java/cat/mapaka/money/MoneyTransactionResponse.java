package cat.mapaka.money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** goalName només s'omple a les files de la cartera GOAL (nom de l'objectiu). */
public record MoneyTransactionResponse(
        UUID id,
        WalletType walletType,
        cat.mapaka.common.TransactionType transactionType,
        BigDecimal amount,
        String description,
        MoneySourceType sourceType,
        Instant createdAt,
        String goalName) {

    public static MoneyTransactionResponse from(MoneyTransaction t, Map<UUID, String> goalNames) {
        return new MoneyTransactionResponse(
                t.getId(), t.getWalletType(), t.getTransactionType(), t.getAmount(),
                t.getDescription(), t.getSourceType(), t.getCreatedAt(), GoalNames.forTransaction(t, goalNames));
    }
}
