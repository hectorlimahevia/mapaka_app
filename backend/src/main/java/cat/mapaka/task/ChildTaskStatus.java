package cat.mapaka.task;

/** Estat visual per al fill (Família+.pdf secció 2.1): AVAILABLE hi és quan encara no hi ha
 * cap TaskCompletion del període vigent. CLAIMED_BY_OTHERS és exclusiu de tasques Extra
 * sense assignació fixa (Prompt 15): un altre fill ja l'ha reclamada i aquest no hi participa. */
public enum ChildTaskStatus {
    AVAILABLE,
    PENDING,
    APPROVED,
    REJECTED,
    CLAIMED_BY_OTHERS
}
