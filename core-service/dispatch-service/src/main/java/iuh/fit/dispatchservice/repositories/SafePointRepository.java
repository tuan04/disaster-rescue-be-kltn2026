package iuh.fit.dispatchservice.repositories;

import iuh.fit.dispatchservice.entity.SafePoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SafePointRepository extends JpaRepository<SafePoint, UUID> {
}
