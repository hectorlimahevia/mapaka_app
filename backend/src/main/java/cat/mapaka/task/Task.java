package cat.mapaka.task;

import cat.mapaka.family.Family;
import cat.mapaka.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "task_type", nullable = false, columnDefinition = "task_type")
    private TaskType taskType;

    @Column(length = 50)
    private String icon;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval;

    @Column(nullable = false)
    private boolean repeatable;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "recurrence_type", nullable = false, columnDefinition = "recurrence_type")
    private RecurrenceType recurrenceType;

    @Column(name = "max_completions_per_period")
    private Integer maxCompletionsPerPeriod;

    /** Només rellevants quan taskType = RESPONSIBILITY — s'apliquen manualment des de
     * "Tasques incompletes", mai de forma automàtica (Prompt 15). */
    @Column(name = "penalty_money_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal penaltyMoneyAmount;

    @Column(name = "penalty_screen_minutes", nullable = false)
    private int penaltyScreenMinutes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
