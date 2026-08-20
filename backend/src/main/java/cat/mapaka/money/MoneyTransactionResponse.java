package cat.mapaka.money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MoneyTransactionResponse(
        UUID id,
        WalletType walletType,
        cat.mapaka.common.TransactionType transactionType,
        BigDecimal amount,
        String description,
        MoneySourceType sourceType,
        Instant createdAt) {

    public static MoneyTransactionResponse from(MoneyTransaction t) {
        return new MoneyTransactionResponse(
                t.getId(), t.getWalletType(), t.getTransactionType(), t.getAmount(),
                t.getDescription(), t.getSourceType(), t.getCreatedAt());
    }
}
