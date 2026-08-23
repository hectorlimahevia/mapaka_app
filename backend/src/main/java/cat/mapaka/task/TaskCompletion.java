package cat.mapaka.task;

import cat.mapaka.child.ChildProfile;
import cat.mapaka.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_completions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile child;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "task_completion_status")
    private TaskCompletionStatus status;

    @Column(name = "submitted_comment")
    private String submittedComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_comment")
    private String reviewComment;

    /** Snapshot de la recompensa vigent en el moment de marcar la tasca (Família+.pdf 13.1) — no canvia mai. */
    @Column(name = "reward_money", nullable = false, precision = 10, scale = 2)
    private BigDecimal rewardMoney;

    @Column(name = "reward_screen_minutes", nullable = false)
    private int rewardScreenMinutes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private int version;
}
