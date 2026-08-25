package cat.mapaka.task;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ChildTaskResponse(
        UUID id,
        String name,
        String description,
        String icon,
        TaskType taskType,
        BigDecimal rewardMoney,
        int rewardScreenMinutes,
        ChildTaskStatus status,
        /** Noms dels fills que participen en la reclamació activa (Prompt 15, tasques Extra
         * col·laboratives) — buit si encara està AVAILABLE. */
        List<String> participantNames) {
}
