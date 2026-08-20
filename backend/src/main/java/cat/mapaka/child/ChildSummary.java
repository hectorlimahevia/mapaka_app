package cat.mapaka.child;

import java.util.UUID;

public record ChildSummary(UUID id, String displayName, String avatar) {

    public static ChildSummary from(ChildProfile child) {
        return new ChildSummary(child.getId(), child.getDisplayName(), child.getAvatar());
    }
}
