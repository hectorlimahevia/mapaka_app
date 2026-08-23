package cat.mapaka.task;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "task_rewards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskReward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "money_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal moneyAmount;

    @Column(name = "screen_minutes", nullable = false)
    private int screenMinutes;

    @Column(nullable = false)
    private boolean active;
}
