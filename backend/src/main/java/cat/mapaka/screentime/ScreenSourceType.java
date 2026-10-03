package cat.mapaka.screentime;

public enum ScreenSourceType {
    DAILY_BASE,
    MONTHLY_BASE,
    TASK,
    BONUS,
    PENALTY,
    USAGE,
    /** Ja no s'escriu; es manté perquè hi pot haver moviments antics al ledger. */
    MANUAL_ADJUSTMENT,
    REVERSAL,
    NFC_SESSION,
    PARENT_SESSION
}
