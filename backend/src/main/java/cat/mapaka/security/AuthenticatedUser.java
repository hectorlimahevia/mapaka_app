package cat.mapaka.security;

import cat.mapaka.user.UserRole;

import java.util.UUID;

/** Principal d'autenticació JWT: dades mínimes necessàries per autoritzar cada petició sense tornar a consultar la BD. */
public record AuthenticatedUser(UUID userId, UUID familyId, UserRole role, UUID childId) {
}
