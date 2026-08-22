package iuh.fit.dispatchservice.repositories;

import iuh.fit.dispatchservice.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {

    @Query(value = """
            SELECT l.id FROM locations l 
            WHERE ST_Covers(l.boundary, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)) = true 
              AND (l.is_active = true OR l.is_active IS NULL) 
            LIMIT 1
            """, nativeQuery = true)
    Optional<UUID> findLocationIdContainingCoordinates(@Param("longitude") double longitude, @Param("latitude") double latitude);

}
