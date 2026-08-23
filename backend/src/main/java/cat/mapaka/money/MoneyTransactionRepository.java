package cat.mapaka.money;

import cat.mapaka.common.TransactionType;
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

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN t.transactionType = :credit THEN t.amount ELSE -t.amount END), 0)
        FROM MoneyTransaction t
        WHERE t.child.id = :childId AND t.walletType = :walletType
        """)
    BigDecimal balanceFor(UUID childId, WalletType walletType, TransactionType credit);

    default BigDecimal balanceFor(UUID childId, WalletType walletType) {
        return balanceFor(childId, walletType, TransactionType.CREDIT);
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
