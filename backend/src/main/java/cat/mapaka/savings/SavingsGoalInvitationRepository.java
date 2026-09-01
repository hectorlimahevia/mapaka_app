package cat.mapaka.savings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SavingsGoalInvitationRepository extends JpaRepository<SavingsGoalInvitation, UUID> {

    /** Comparar l'enum natiu de Postgres amb un `:status` fixat evita el bug del cast
     * incorrecte (::AllowanceStatus en lloc de ::allowance_status) que ja va donar
     * un 500 real amb un literal JPQL a MonthlyAllowanceRepository. */
    @Query("SELECT i FROM SavingsGoalInvitation i JOIN FETCH i.sourceGoal JOIN FETCH i.inviterChild " +
            "WHERE i.invitedChild.id = :childId AND i.status = :status")
    List<SavingsGoalInvitation> findByInvitedChildIdAndStatus(@Param("childId") UUID childId, @Param("status") GoalInvitationStatus status);
}
