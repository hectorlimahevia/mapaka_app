package cat.mapaka.adjustment;

/** On va una bonificació en diners: RULE manté el repartiment habitual del fill (gastar,
 * estalvi i objectius); les altres tres l'envien directament a la cartera triada. */
public enum AdjustmentDestination {
    RULE,
    SPENDING,
    SAVINGS,
    CUSTOM
}
