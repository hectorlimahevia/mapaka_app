package cat.mapaka.savings;

import cat.mapaka.child.ChildProfile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** Invitació a compartir un objectiu d'estalvi entre germans: `sourceGoal` és l'objectiu de qui
 * convida i marca les condicions (import, percentatge, imatge) que es clonaran en un SavingsGoal
 * propi per a qui accepti — mai un únic objectiu amb múltiples propietaris, cada germà manté
 * el seu propi progrés (ledger propi, wallet GOAL pròpia). */
@Entity
@Table(name = "savings_goal_invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsGoalInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "shared_goal_group_id", nullable = false)
    private UUID sharedGoalGroupId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_goal_id", nullable = false)
    private SavingsGoal sourceGoal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inviter_child_id", nullable = false)
    private ChildProfile inviterChild;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_child_id", nullable = false)
    private ChildProfile invitedChild;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "goal_invitation_status")
    private GoalInvitationStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;
}
