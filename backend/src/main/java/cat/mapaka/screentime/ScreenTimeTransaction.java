package cat.mapaka.screentime;

import cat.mapaka.child.ChildProfile;
import cat.mapaka.common.TransactionType;
import cat.mapaka.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "screen_time_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreenTimeTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile child;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "transaction_type", nullable = false, columnDefinition = "transaction_type")
    private TransactionType transactionType;

    @Column(nullable = false)
    private int minutes;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "source_type", nullable = false, columnDefinition = "screen_source_type")
    private ScreenSourceType sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    /** Data local de la família a la qual pertany el moviment (restricció idempotent de la generació diària). */
    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    /** Null quan el moviment es genera sense usuari autenticat (DAILY_BASE, NFC_SESSION). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "reversed_transaction_id")
    private UUID reversedTransactionId;
}
