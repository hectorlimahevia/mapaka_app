package cat.mapaka.task;

import java.math.BigDecimal;
import java.util.UUID;

/** Una tasca de Responsabilitat, per a un fill concret, la finestra de recurrència de la
 * qual ja ha vençut sense cap finalització aprovada (Prompt 15) — llest perquè el PARENT
 * decideixi aplicar-hi la penalització manualment. */
public record IncompleteTaskResponse(
        UUID taskId, String taskName, UUID childId, String childDisplayName,
        BigDecimal penaltyMoneyAmount, int penaltyScreenMinutes) {
}
