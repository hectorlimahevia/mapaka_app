package cat.mapaka.screentime;

import cat.mapaka.child.ChildProfile;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "screen_time_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreenTimeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private ChildProfile child;

    /** 0 = diumenge ... 6 = dissabte. Null = aplica tots els dies. */
    @Column
    private Integer weekday;

    @Column(name = "base_minutes", nullable = false)
    private int baseMinutes;

    @Column(name = "maximum_minutes")
    private Integer maximumMinutes;

    @Column(name = "rollover_enabled", nullable = false)
    private boolean rolloverEnabled;

    @Column(name = "rollover_max_minutes")
    private Integer rolloverMaxMinutes;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(nullable = false)
    private boolean active;
}
