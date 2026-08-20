package cat.mapaka.adjustment;

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
@Table(name = "adjustments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Adjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile child;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "adjustment_type", nullable = false, columnDefinition = "adjustment_type")
    private AdjustmentType adjustmentType;

    @Column(name = "money_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal moneyAmount;

    @Column(name = "savings_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal savingsAmount;

    @Column(name = "screen_minutes", nullable = false)
    private int screenMinutes;

    @Column(nullable = false)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
