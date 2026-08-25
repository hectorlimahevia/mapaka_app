package cat.mapaka.task;

import java.util.List;
import java.util.UUID;

/** Buit (`[]`) si el fill no ha rebut ajuda — només té efecte real en tasques Extra
 * (Prompt 15); s'ignora en tasques amb assignació fixa. */
public record CompleteTaskRequest(List<UUID> collaboratorChildIds) {
}
