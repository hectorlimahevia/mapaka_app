package cat.mapaka.money;

public enum MoneySourceType {
    MONTHLY_ALLOWANCE,
    TASK,
    BONUS,
    PENALTY,
    PURCHASE,
    SAVINGS_TRANSFER,
    /** Ja no s'escriu; es manté perquè hi pot haver moviments antics al ledger. */
    MANUAL_ADJUSTMENT,
    SETTLEMENT,
    REVERSAL,
    GOAL_CONTRIBUTION,
    DONATION,
    TASK_PENALTY
}
