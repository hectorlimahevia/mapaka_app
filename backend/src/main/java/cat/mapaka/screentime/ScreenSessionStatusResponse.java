package cat.mapaka.screentime;

import cat.mapaka.child.ChildSummary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Resposta d'un toc/aturada de sessió NFC o de l'inici/consulta d'una sessió manual.
 * familyChildren només s'omple quan status=CLOSED (moment en què cal mostrar el selector
 * "Qui ha jugat?"), per no exposar un endpoint públic addicional que llisti fills per
 * family_id. startedAt permet recalcular el cronòmetre si la pantalla es recarrega
 * mentre la sessió és ACTIVE.
 */
public record ScreenSessionStatusResponse(
        UUID sessionId,
        ScreenSessionStatus status,
        Integer elapsedSeconds,
        List<ChildSummary> familyChildren,
        Instant startedAt) {
}
