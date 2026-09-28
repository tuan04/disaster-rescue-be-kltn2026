package iuh.fit.dispatchservice.repositories;

import iuh.fit.dispatchservice.dtos.projection.HazardSpatialProjection;
import iuh.fit.dispatchservice.entity.HazardReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface HazardReportRepository extends JpaRepository<HazardReport, UUID> {

    @Query(value = """
            SELECT hr.id AS id,
                   hr.hazard_type AS hazardType,
                   ST_Y(mp.location) AS latitude,
                   ST_X(mp.location) AS longitude
            FROM hazard_reports hr
            JOIN map_points mp ON mp.id = hr.id
            WHERE mp.is_visible = true
              AND hr.status = 'ACTIVE'
              AND mp.location && ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)
            """, nativeQuery = true)
    List<HazardSpatialProjection> findActiveHazardsInBoundingBox(
            @Param("minLng") double minLng,
            @Param("minLat") double minLat,
            @Param("maxLng") double maxLng,
            @Param("maxLat") double maxLat
    );
}

