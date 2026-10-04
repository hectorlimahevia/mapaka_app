package cat.mapaka.settlement;

import java.math.BigDecimal;
import java.util.UUID;

/** Resum d'un fill en un mes: el que ha ENTRAT aquell mes (no el saldo acumulat).
 * `total` = paga base + tasques + bonificacions − penalitzacions, i també
 * = `spending` + `savings` + `goals`. `current` és el mes en curs (calculat fins ara);
 * `allowancePending` indica que la paga d'aquest mes encara està per confirmar. */
public record MonthlySummaryResponse(
        UUID childId, String childDisplayName, String avatarColor, String avatarIcon,
        int year, int month, boolean current, boolean allowancePending,
        BigDecimal total, BigDecimal baseAllowance, BigDecimal extraEarnings, BigDecimal bonuses,
        BigDecimal penalties, BigDecimal spending, BigDecimal savings, BigDecimal goals) {
}
