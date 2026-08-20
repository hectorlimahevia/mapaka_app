package cat.mapaka.child;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChildProfileRepository extends JpaRepository<ChildProfile, UUID> {

    Optional<ChildProfile> findByUserId(UUID userId);

    /**
     * Carrega user i user.family d'un cop: evita LazyInitializationException quan el
     * ChildProfile es fa servir fora d'una transacció (p. ex. a ChildAccessService,
     * consultat des de múltiples controllers no transaccionals).
     */
    @Query("SELECT c FROM ChildProfile c JOIN FETCH c.user u JOIN FETCH u.family WHERE c.id = :id")
    Optional<ChildProfile> findByIdFetchUserAndFamily(UUID id);

    @Query("SELECT c FROM ChildProfile c WHERE c.user.family.id = :familyId")
    List<ChildProfile> findAllByFamilyId(UUID familyId);

    @Query("SELECT c FROM ChildProfile c JOIN FETCH c.user WHERE c.user.family.id = :familyId AND c.active = true")
    List<ChildProfile> findAllActiveByFamilyIdFetchUser(UUID familyId);
}
