package cat.mapaka.family;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "families")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Family {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "task_approval_required", nullable = false)
    private boolean taskApprovalRequired;

    @Column(name = "notify_pending_approvals_enabled", nullable = false)
    private boolean notifyPendingApprovalsEnabled;

    @Column(name = "allow_savings_transfer", nullable = false)
    private boolean allowSavingsTransfer;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
