package cat.mapaka.auth;

import cat.mapaka.user.UserRole;

import java.util.UUID;

public record AuthResponse(String accessToken, UUID userId, UUID familyId, UserRole role, UUID childId) {
}
