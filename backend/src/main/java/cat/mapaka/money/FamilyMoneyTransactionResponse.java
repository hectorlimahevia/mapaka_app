package cat.mapaka.money;

import cat.mapaka.common.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** goalName només s'omple a les files de la cartera GOAL (nom de l'objectiu). */
public record FamilyMoneyTransactionResponse(
        UUID id,
        UUID childId,
        String childDisplayName,
        WalletType walletType,
        TransactionType transactionType,
        BigDecimal amount,
        String description,
        MoneySourceType sourceType,
        Instant createdAt,
        String goalName) {

    public static FamilyMoneyTransactionResponse from(MoneyTransaction t, Map<UUID, String> goalNames) {
        return new FamilyMoneyTransactionResponse(
                t.getId(), t.getChild().getId(), t.getChild().getDisplayName(), t.getWalletType(), t.getTransactionType(),
                t.getAmount(), t.getDescription(), t.getSourceType(), t.getCreatedAt(), GoalNames.forTransaction(t, goalNames));
    }
}
