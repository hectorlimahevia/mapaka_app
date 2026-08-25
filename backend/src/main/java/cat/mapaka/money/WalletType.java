package cat.mapaka.money;

public enum WalletType {
    SPENDING,
    SAVINGS,
    /** Contribucions (repartiment o donacions) a un objectiu d'estalvi concret — bucle
     * separat de SAVINGS perquè el progrés d'un objectiu no es confongui amb l'estalvi
     * general del fill (Prompt 15, targeta híbrida de Resum familiar). */
    GOAL
}
