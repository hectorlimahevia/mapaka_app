package cat.mapaka.money;

import java.math.BigDecimal;

public record WalletResponse(BigDecimal spendingBalance, BigDecimal savingsBalance, BigDecimal total) {
}
