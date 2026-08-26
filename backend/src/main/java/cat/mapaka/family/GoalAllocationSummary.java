package cat.mapaka.family;

import java.math.BigDecimal;

/** Un tram de la barra segmentada de la targeta híbrida de Resum familiar (Prompt 15) —
 * un per cada objectiu ACTIVE del fill. */
public record GoalAllocationSummary(String name, BigDecimal allocationPercentage, BigDecimal currentAmount) {
}
