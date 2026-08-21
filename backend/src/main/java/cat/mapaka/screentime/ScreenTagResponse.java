package cat.mapaka.screentime;

import java.time.Instant;
import java.util.UUID;

public record ScreenTagResponse(UUID id, String token, boolean active, Instant createdAt, String url) {

    public static ScreenTagResponse from(ScreenTag tag, String frontendUrl) {
        return new ScreenTagResponse(
                tag.getId(), tag.getToken(), tag.isActive(), tag.getCreatedAt(), frontendUrl + "/screen/" + tag.getToken());
    }
}
