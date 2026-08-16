package iuh.fit.dispatchservice.repositories;

import iuh.fit.dispatchservice.entity.MapPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MapPointRepository extends JpaRepository<MapPoint, UUID> {

}
