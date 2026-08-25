package cat.mapaka.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateTaskRequest(
        @NotBlank String name,
        String description,
        @NotNull TaskType taskType,
        String icon,
        boolean requiresApproval,
        @NotNull RecurrenceType recurrenceType,
        @NotNull BigDecimal rewardMoney,
        int rewardScreenMinutes,
        /** Només per a taskType = RESPONSIBILITY — omès o null equival a 0 (Prompt 15). */
        BigDecimal penaltyMoneyAmount,
        Integer penaltyScreenMinutes,
        /** Ignorat per a taskType = EXTRA: no té assignació fixa, és visible per a tota la família. */
        @NotNull List<UUID> childIds) {
}
