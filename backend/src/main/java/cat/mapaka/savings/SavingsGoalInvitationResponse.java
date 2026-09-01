package cat.mapaka.savings;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SavingsGoalInvitationResponse(
        UUID id, UUID sourceGoalId, String goalName, BigDecimal targetAmount, BigDecimal allocationPercentage,
        String imageUrl, String inviterChildName, Instant createdAt) {

    public static SavingsGoalInvitationResponse from(SavingsGoalInvitation invitation) {
        SavingsGoal source = invitation.getSourceGoal();
        return new SavingsGoalInvitationResponse(
                invitation.getId(), source.getId(), source.getName(), source.getTargetAmount(),
                source.getAllocationPercentage(), source.getImageUrl(),
                invitation.getInviterChild().getDisplayName(), invitation.getCreatedAt());
    }
}
