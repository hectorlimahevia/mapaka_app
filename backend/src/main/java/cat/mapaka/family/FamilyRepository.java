package cat.mapaka.family;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FamilyRepository extends JpaRepository<Family, UUID> {

    List<Family> findByActiveTrueAndNameContainingIgnoreCase(String name, Limit limit);
}
