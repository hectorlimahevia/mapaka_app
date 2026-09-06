package cat.mapaka.family;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamilyRepository extends JpaRepository<Family, UUID> {

    List<Family> findByActiveTrueAndNameContainingIgnoreCase(String name, Limit limit);

    Optional<Family> findByActiveTrueAndFamilyCodeIgnoreCase(String familyCode);

    boolean existsByFamilyCode(String familyCode);
}
