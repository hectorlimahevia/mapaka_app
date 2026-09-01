package cat.mapaka.allowance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AllowanceRuleRepository extends JpaRepository<AllowanceRule, UUID> {

    Optional<AllowanceRule> findByChildIdAndActiveTrue(UUID childId);

    List<AllowanceRule> findByFamilyIdAndChildIsNullAndActiveTrueOrderByMinAgeAsc(UUID familyId);

    void deleteByChildId(UUID childId);
}
