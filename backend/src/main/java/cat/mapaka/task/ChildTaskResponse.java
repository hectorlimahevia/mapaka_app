package cat.mapaka.task;

import java.math.BigDecimal;
import java.util.UUID;

public record ChildTaskResponse(
        UUID id,
        String name,
        String description,
        String icon,
        TaskType taskType,
        BigDecimal rewardMoney,
        BigDecimal rewardSavings,
        int rewardScreenMinutes,
        ChildTaskStatus status) {
}
