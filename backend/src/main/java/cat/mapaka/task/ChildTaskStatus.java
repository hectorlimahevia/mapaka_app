package cat.mapaka.task;

/** Estat visual per al fill (Família+.pdf secció 2.1): AVAILABLE hi és quan encara no hi ha cap TaskCompletion del període vigent. */
public enum ChildTaskStatus {
    AVAILABLE,
    PENDING,
    APPROVED,
    REJECTED
}
