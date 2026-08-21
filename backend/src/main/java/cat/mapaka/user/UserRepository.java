package cat.mapaka.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByFamilyIdAndUsername(UUID familyId, String username);

    List<User> findAllByFamilyIdAndUsernameStartingWith(UUID familyId, String usernamePrefix);

    Optional<User> findFirstByFamilyIdAndRoleOrderByCreatedAtAsc(UUID familyId, UserRole role);

    List<User> findAllByFamilyIdAndRoleAndActiveTrue(UUID familyId, UserRole role);

    /** Evita LazyInitializationException quan cal comparar user.family.id fora de transacció. */
    @Query("SELECT u FROM User u JOIN FETCH u.family WHERE u.id = :id")
    Optional<User> findByIdFetchFamily(UUID id);
}
