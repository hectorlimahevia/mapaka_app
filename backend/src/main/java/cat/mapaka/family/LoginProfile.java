package cat.mapaka.family;

import cat.mapaka.child.ChildProfile;
import cat.mapaka.user.User;
import cat.mapaka.user.UserRole;

import java.util.UUID;

/**
 * Perfil mínim per al selector "Qui ets?" del login (secció 39, ara compartit per tots dos
 * rols — Prompt 6) — només identificador, nom, avatar, username i rol; mai saldos, tasques
 * ni cap altra dada. L'id només serveix perquè un PARENT ja autenticat pugui triar a qui
 * reseteja el PIN des de Configuració (PATCH /api/users/{id}/pin) — no és sensible per si sol.
 */
public record LoginProfile(UUID id, String username, String displayName, String avatar, UserRole role) {

    public static LoginProfile fromChild(ChildProfile child) {
        return new LoginProfile(
                child.getUser().getId(), child.getUser().getUsername(), child.getDisplayName(), child.getAvatar(), UserRole.CHILD);
    }

    public static LoginProfile fromParent(User user) {
        return new LoginProfile(user.getId(), user.getUsername(), user.getDisplayName(), null, UserRole.PARENT);
    }
}
