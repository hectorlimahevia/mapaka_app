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
        @NotNull BigDecimal rewardSavings,
        int rewardScreenMinutes,
        @NotNull List<UUID> childIds) {
}
