package cat.mapaka.screentime;

import cat.mapaka.child.ChildSummary;

import java.util.List;
import java.util.UUID;

/**
 * Resposta d'un toc/aturada de sessió NFC. familyChildren només s'omple quan status=STOPPED
 * (moment en què cal mostrar el selector "Qui ha jugat?"), per no exposar un endpoint públic
 * addicional que llisti fills per family_id.
 */
public record ScreenSessionStatusResponse(
        UUID sessionId,
        ScreenSessionStatus status,
        Integer elapsedSeconds,
        List<ChildSummary> familyChildren) {
}
