package cat.mapaka.child;

/**
 * Perfil mínim per al selector "Qui ets?" del login infantil (Família+.pdf secció 39) —
 * només nom, avatar i username; mai saldos, tasques ni cap altra dada.
 */
public record ChildLoginProfile(String username, String displayName, String avatar) {

    public static ChildLoginProfile from(ChildProfile child) {
        return new ChildLoginProfile(child.getUser().getUsername(), child.getDisplayName(), child.getAvatar());
    }
}
