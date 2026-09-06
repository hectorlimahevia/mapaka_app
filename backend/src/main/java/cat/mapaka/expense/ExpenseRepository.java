package cat.mapaka.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    /** Comparar l'enum natiu de Postgres amb un `:status` fixat (mai un literal JPQL) —
     * un literal hi generaria un cast SQL incorrecte (::ExpenseStatus en lloc del tipus
     * real de Postgres), el mateix bug real ja trobat a MonthlyAllowanceRepository. */
    @Query("SELECT e FROM Expense e JOIN FETCH e.child c JOIN FETCH c.user u " +
            "WHERE u.family.id = :familyId AND e.status = :status ORDER BY e.createdAt ASC")
    List<Expense> findByFamilyIdAndStatus(@Param("familyId") UUID familyId, @Param("status") ExpenseStatus status);

    List<Expense> findByChildIdAndStatusOrderByCreatedAtDesc(UUID childId, ExpenseStatus status);

    @Query("SELECT e FROM Expense e JOIN FETCH e.child c JOIN FETCH c.user u JOIN FETCH u.family WHERE e.id = :id")
    Optional<Expense> findByIdFetchChildAndFamily(@Param("id") UUID id);
}
