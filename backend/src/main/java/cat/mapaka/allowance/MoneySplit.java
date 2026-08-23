package cat.mapaka.allowance;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Reparteix un import total entre gastar i estalviar segons un percentatge de gastar —
 * mateixa fórmula que ja feia servir AllowanceGenerationService, ara compartida perquè
 * l'aprovació de tasques i els ajustos manuals també la necessiten (Prompt 9/10 corregits:
 * el reparto es calcula quan es genera el moviment real, mai quan l'usuari l'introdueix). */
public record MoneySplit(BigDecimal spending, BigDecimal savings) {

    public static MoneySplit of(BigDecimal total, BigDecimal spendingPercentage) {
        BigDecimal spending = total.multiply(spendingPercentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal savings = total.subtract(spending);
        return new MoneySplit(spending, savings);
    }
}
