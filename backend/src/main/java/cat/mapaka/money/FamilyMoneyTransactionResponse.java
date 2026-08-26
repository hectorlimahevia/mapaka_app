package cat.mapaka.money;

import cat.mapaka.common.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FamilyMoneyTransactionResponse(
        UUID id,
        UUID childId,
        String childDisplayName,
        WalletType walletType,
        TransactionType transactionType,
        BigDecimal amount,
        String description,
        MoneySourceType sourceType,
        Instant createdAt) {

    public static FamilyMoneyTransactionResponse from(MoneyTransaction t) {
        return new FamilyMoneyTransactionResponse(
                t.getId(), t.getChild().getId(), t.getChild().getDisplayName(), t.getWalletType(), t.getTransactionType(),
                t.getAmount(), t.getDescription(), t.getSourceType(), t.getCreatedAt());
    }
}
