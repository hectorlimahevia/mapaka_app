package cat.mapaka.screentime;

import java.time.Instant;
import java.util.UUID;

public record ScreenTagResponse(UUID id, String token, boolean active, Instant createdAt, String url) {

    public static ScreenTagResponse from(ScreenTag tag, String publicBaseUrl) {
        return new ScreenTagResponse(
                tag.getId(), tag.getToken(), tag.isActive(), tag.getCreatedAt(), publicBaseUrl + "/screen/" + tag.getToken());
    }
}
