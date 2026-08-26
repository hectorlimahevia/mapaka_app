package cat.mapaka.allowance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface MonthlyAllowanceRepository extends JpaRepository<MonthlyAllowance, UUID> {

    Optional<MonthlyAllowance> findByChildIdAndYearAndMonth(UUID childId, int year, int month);

    /** Camp per a la campaneta d'alertes de Resum familiar (Prompt 15, checklist 18):
     * "ja generat aquest mes" es dedueix directament d'aquí, sense cap taula/camp nou. */
    boolean existsByChild_User_Family_IdAndYearAndMonth(UUID familyId, int year, int month);

    /** Evita LazyInitializationException quan cal comparar allowance.child.user.family.id
     * fora de transacció (controller). */
    @Query("SELECT a FROM MonthlyAllowance a JOIN FETCH a.child c JOIN FETCH c.user u JOIN FETCH u.family WHERE a.id = :id")
    Optional<MonthlyAllowance> findByIdFetchFamily(UUID id);
}
