package cat.mapaka.allowance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AllowanceRuleRepository extends JpaRepository<AllowanceRule, UUID> {
}
