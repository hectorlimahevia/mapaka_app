package cat.mapaka.settlement;

import java.math.BigDecimal;
import java.util.UUID;

public record MonthlySettlementResponse(
        UUID id, UUID childId, String childDisplayName, int year, int month,
        BigDecimal baseAllowance, BigDecimal extraEarnings, BigDecimal bonuses, BigDecimal penalties,
        BigDecimal savings, BigDecimal payableAmount, SettlementStatus status) {

    public static MonthlySettlementResponse from(MonthlySettlement s) {
        return new MonthlySettlementResponse(
                s.getId(), s.getChild().getId(), s.getChild().getDisplayName(), s.getYear(), s.getMonth(),
                s.getBaseAllowance(), s.getExtraEarnings(), s.getBonuses(), s.getPenalties(),
                s.getSavings(), s.getPayableAmount(), s.getStatus());
    }
}
