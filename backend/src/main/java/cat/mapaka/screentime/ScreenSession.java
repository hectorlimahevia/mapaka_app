package cat.mapaka.screentime;

import cat.mapaka.family.Family;
import cat.mapaka.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "screen_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreenSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** Null per a una sessió manual (iniciada des de la sessió del pare/mare, sense objecte físic). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_tag_id")
    private ScreenTag screenTag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    /** Null per a una sessió NFC (l'objecte físic identifica la família, no cap usuari). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by")
    private User startedBy;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "elapsed_seconds")
    private Integer elapsedSeconds;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "screen_session_status")
    private ScreenSessionStatus status;
}
