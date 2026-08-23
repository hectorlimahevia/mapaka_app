package cat.mapaka.task;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PendingApprovalResponse(
        UUID taskCompletionId,
        UUID childId,
        String childName,
        String taskName,
        BigDecimal rewardMoney,
        int rewardScreenMinutes,
        Instant completedAt) {

    public static PendingApprovalResponse from(TaskCompletion c) {
        return new PendingApprovalResponse(
                c.getId(), c.getChild().getId(), c.getChild().getDisplayName(), c.getTask().getName(),
                c.getRewardMoney(), c.getRewardScreenMinutes(), c.getCompletedAt());
    }
}
