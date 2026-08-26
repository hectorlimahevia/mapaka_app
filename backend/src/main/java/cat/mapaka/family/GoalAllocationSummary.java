package cat.mapaka.family;

import java.math.BigDecimal;
import java.util.UUID;

/** Un tram de la barra segmentada de la targeta híbrida de Resum familiar (Prompt 15) —
 * un per cada objectiu del fill, sigui quin sigui el seu estat (un objectiu COMPLETED
 * continua sent diner real i ha de seguir comptant al "Total"). Porta `goalId` perquè el
 * PARENT pugui registrar-hi una donació (secció 8.2) sense una crida addicional per
 * resoldre l'objectiu. */
public record GoalAllocationSummary(UUID goalId, String name, BigDecimal allocationPercentage, BigDecimal currentAmount) {
}
