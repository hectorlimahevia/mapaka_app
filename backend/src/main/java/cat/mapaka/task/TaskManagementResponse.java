package cat.mapaka.task;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record TaskManagementResponse(
        UUID id,
        String name,
        String description,
        TaskType taskType,
        String icon,
        boolean requiresApproval,
        boolean active,
        RecurrenceType recurrenceType,
        BigDecimal rewardMoney,
        int rewardScreenMinutes,
        List<AssignedChild> assignedChildren) {

    public record AssignedChild(UUID childId, String displayName) {
    }
}
