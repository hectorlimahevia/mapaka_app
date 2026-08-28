package cat.mapaka.family;

import cat.mapaka.savings.SavingsGoalStatus;

import java.math.BigDecimal;
import java.util.UUID;

/** Un objectiu d'un fill dins la targeta híbrida de Resum familiar (Prompt 15) — un per
 * cada objectiu, sigui quin sigui el seu estat (un objectiu COMPLETED continua sent diner
 * real i ha de seguir comptant al "Total"). Porta `goalId` perquè el PARENT pugui
 * registrar-hi una donació (secció 8.2) sense una crida addicional per resoldre l'objectiu.
 * `targetAmount`/`status` alimenten la barra de progrés i el tractament daurat dels
 * objectius assolits (ajust posterior, mateix tractament que la pantalla Objectius del fill). */
public record GoalAllocationSummary(
        UUID goalId, String name, BigDecimal allocationPercentage, BigDecimal currentAmount,
        BigDecimal targetAmount, SavingsGoalStatus status) {
}
