package cat.mapaka.adjustment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdjustmentRepository extends JpaRepository<Adjustment, UUID> {
}
