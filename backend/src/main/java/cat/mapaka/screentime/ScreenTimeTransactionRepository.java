package cat.mapaka.screentime;

import cat.mapaka.common.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ScreenTimeTransactionRepository extends JpaRepository<ScreenTimeTransaction, UUID> {

    List<ScreenTimeTransaction> findByChildIdOrderByCreatedAtDesc(UUID childId);

    boolean existsByChildIdAndOccurredOnAndSourceType(UUID childId, LocalDate occurredOn, ScreenSourceType sourceType);

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN t.transactionType = :credit THEN t.minutes ELSE -t.minutes END), 0)
        FROM ScreenTimeTransaction t
        WHERE t.child.id = :childId
        """)
    int balanceFor(UUID childId, TransactionType credit);

    default int balanceFor(UUID childId) {
        return balanceFor(childId, TransactionType.CREDIT);
    }
}
