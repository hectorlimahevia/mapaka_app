package cat.mapaka.settlement;

import cat.mapaka.child.ChildProfile;
import cat.mapaka.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "monthly_settlements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlySettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile child;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Column(name = "base_allowance", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseAllowance;

    @Column(name = "extra_earnings", nullable = false, precision = 10, scale = 2)
    private BigDecimal extraEarnings;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal bonuses;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal penalties;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal savings;

    @Column(name = "payable_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal payableAmount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "settlement_status")
    private SettlementStatus status;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private User closedBy;

    @Version
    @Column(nullable = false)
    private int version;
}
