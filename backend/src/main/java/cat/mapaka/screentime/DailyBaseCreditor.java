package cat.mapaka.screentime;

import cat.mapaka.child.ChildProfile;
import cat.mapaka.common.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Transacció pròpia (REQUIRES_NEW) perquè una violació de la restricció idempotent
 * (child_id, occurred_on) — dues peticions gairebé simultànies el primer cop del dia —
 * només avorti aquest INSERT concret, no la transacció que l'ha demanat.
 */
@Service
class DailyBaseCreditor {

    private final ScreenTimeTransactionRepository screenTimeTransactionRepository;

    DailyBaseCreditor(ScreenTimeTransactionRepository screenTimeTransactionRepository) {
        this.screenTimeTransactionRepository = screenTimeTransactionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void credit(ChildProfile child, LocalDate today, int baseMinutes) {
        screenTimeTransactionRepository.save(ScreenTimeTransaction.builder()
                .child(child)
                .transactionType(TransactionType.CREDIT)
                .minutes(baseMinutes)
                .description("Temps base diari")
                .sourceType(ScreenSourceType.DAILY_BASE)
                .occurredOn(today)
                .createdBy(null)
                .build());
    }
}
