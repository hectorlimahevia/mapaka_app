package cat.mapaka.allowance;

import java.math.BigDecimal;
import java.util.UUID;

public record MonthlyAllowanceResponse(
        UUID id, UUID childId, String childDisplayName, int year, int month,
        BigDecimal grossAmount, BigDecimal spendingAmount, BigDecimal savingsAmount, AllowanceStatus status) {

    public static MonthlyAllowanceResponse from(MonthlyAllowance allowance) {
        return new MonthlyAllowanceResponse(
                allowance.getId(), allowance.getChild().getId(), allowance.getChild().getDisplayName(),
                allowance.getYear(), allowance.getMonth(), allowance.getGrossAmount(),
                allowance.getSpendingAmount(), allowance.getSavingsAmount(), allowance.getStatus());
    }
}
