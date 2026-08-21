package cat.mapaka.screentime;

import java.time.Instant;
import java.util.UUID;

public record NegativeBalanceSessionResponse(UUID childId, String childName, int assignedSeconds, Instant occurredAt) {

    public static NegativeBalanceSessionResponse from(ScreenSessionParticipant p) {
        return new NegativeBalanceSessionResponse(
                p.getChild().getId(), p.getChild().getDisplayName(), p.getAssignedSeconds(), p.getCreatedAt());
    }
}
