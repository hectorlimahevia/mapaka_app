package cat.mapaka.screentime;

import java.util.List;
import java.util.UUID;

public record AssignSessionResponse(UUID sessionId, List<ParticipantResult> participants) {

    public record ParticipantResult(
            UUID childId, String displayName, int assignedSeconds, int resultingBalanceMinutes, boolean negativeBalance) {
    }
}
