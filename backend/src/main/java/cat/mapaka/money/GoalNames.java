package cat.mapaka.money;

import cat.mapaka.savings.SavingsGoal;
import cat.mapaka.savings.SavingsGoalRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Noms dels objectius que apareixen a una llista de moviments: les files de la cartera
 * GOAL guarden l'id de l'objectiu a source_id, i la llista mostra el nom, no l'id. */
public final class GoalNames {

    private GoalNames() {
    }

    public static Map<UUID, String> of(List<MoneyTransaction> transactions, SavingsGoalRepository savingsGoalRepository) {
        Set<UUID> goalIds = transactions.stream()
                .filter(t -> t.getWalletType() == WalletType.GOAL && t.getSourceId() != null)
                .map(MoneyTransaction::getSourceId)
                .collect(Collectors.toSet());
        if (goalIds.isEmpty()) {
            return Map.of();
        }
        return savingsGoalRepository.findAllById(goalIds).stream()
                .collect(Collectors.toMap(SavingsGoal::getId, SavingsGoal::getName));
    }

    static String forTransaction(MoneyTransaction t, Map<UUID, String> goalNames) {
        return t.getWalletType() == WalletType.GOAL && t.getSourceId() != null ? goalNames.get(t.getSourceId()) : null;
    }
}
