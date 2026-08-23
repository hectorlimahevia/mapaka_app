package cat.mapaka.allowance;

import java.math.BigDecimal;
import java.util.UUID;

public record AllowanceRuleResponse(
        UUID id, Integer minAge, Integer maxAge, BigDecimal monthlyAmount,
        BigDecimal spendingPercentage, BigDecimal savingsPercentage) {

    public static AllowanceRuleResponse from(AllowanceRule rule) {
        return new AllowanceRuleResponse(
                rule.getId(), rule.getMinAge(), rule.getMaxAge(), rule.getMonthlyAmount(),
                rule.getSpendingPercentage(), rule.getSavingsPercentage());
    }
}
