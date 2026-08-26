package cat.mapaka.money;

import cat.mapaka.common.TransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MoneyTransactionRepository extends JpaRepository<MoneyTransaction, UUID> {

    List<MoneyTransaction> findByChildIdOrderByCreatedAtDesc(UUID childId);

    @Query("""
        SELECT t FROM MoneyTransaction t JOIN FETCH t.child
        WHERE t.child.user.family.id = :familyId ORDER BY t.createdAt DESC
        """)
    List<MoneyTransaction> findByFamilyIdOrderByCreatedAtDesc(UUID familyId);

    /** `childId` opcional; `from`/`to` sempre concrets — el controller hi substitueix
     * EPOCH/un futur llunyà quan el filtre de període no s'aplica, per evitar
     * l'ambigüitat de tipus que dona Postgres amb un `Instant` nul dins d'un "IS NULL
     * OR" (Prompt 15, Resum familiar 7.2). El `Pageable` és qui limita l'historial
     * complet quan es demana des de la pantalla paginada. */
    @Query("""
        SELECT t FROM MoneyTransaction t JOIN FETCH t.child
        WHERE t.child.user.family.id = :familyId
        AND (:childId IS NULL OR t.child.id = :childId)
        AND t.createdAt >= :from AND t.createdAt < :to
        ORDER BY t.createdAt DESC
        """)
    List<MoneyTransaction> findByFamilyIdFiltered(UUID familyId, UUID childId, Instant from, Instant to, Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN t.transactionType = :credit THEN t.amount ELSE -t.amount END), 0)
        FROM MoneyTransaction t
        WHERE t.child.id = :childId AND t.walletType = :walletType
        """)
    BigDecimal balanceFor(UUID childId, WalletType walletType, TransactionType credit);

    default BigDecimal balanceFor(UUID childId, WalletType walletType) {
        return balanceFor(childId, walletType, TransactionType.CREDIT);
    }

    /** Progrés d'un objectiu concret (repartiment + donacions) — mai el total d'estalvi
     * compartit del fill (Prompt 15: cada objectiu té el seu propi bucle GOAL). El
     * walletType es passa com a paràmetre (mai un literal inline a la JPQL) perquè
     * Hibernate el vinculi amb el tipus NAMED_ENUM correcte (wallet_type, no "wallettype"). */
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN t.transactionType = :credit THEN t.amount ELSE -t.amount END), 0)
        FROM MoneyTransaction t
        WHERE t.walletType = :walletType AND t.sourceId = :goalId
        """)
    BigDecimal goalProgress(UUID goalId, WalletType walletType, TransactionType credit);

    default BigDecimal goalProgress(UUID goalId) {
        return goalProgress(goalId, WalletType.GOAL, TransactionType.CREDIT);
    }

    /** Suma d'un tipus d'origen concret dins d'una finestra de temps — fa servir el tancament
     * mensual (settlement) per desglossar d'on ve el que s'ha pagat aquell mes. */
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM MoneyTransaction t
        WHERE t.child.id = :childId AND t.walletType = :walletType AND t.sourceType = :sourceType
        AND t.transactionType = :transactionType AND t.createdAt >= :from AND t.createdAt < :to
        """)
    BigDecimal sumBySource(
            UUID childId, WalletType walletType, MoneySourceType sourceType, TransactionType transactionType,
            Instant from, Instant to);
}
