package cat.mapaka.child;

import cat.mapaka.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

@Entity
@Table(name = "child_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChildProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column
    private String avatar;

    @Column(name = "color_theme", length = 20)
    private String colorTheme;

    /** NULL vol dir "mostra la inicial del nom" (Prompt 15) — icona d'un set tancat predefinit. */
    @Column(name = "avatar_icon", length = 50)
    private String avatarIcon;

    @Column(name = "allowance_enabled", nullable = false)
    private boolean allowanceEnabled;

    @Column(name = "screen_time_enabled", nullable = false)
    private boolean screenTimeEnabled;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** L'edat mai es persisteix — es calcula sempre a partir de birthDate (Família+.pdf 7.3). */
    @Transient
    public int getAge() {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
