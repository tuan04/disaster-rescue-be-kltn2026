package iuh.fit.dispatchservice.repositories;

import iuh.fit.dispatchservice.dtos.projection.MapPointProjection;
import iuh.fit.dispatchservice.entity.MapPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MapPointRepository extends JpaRepository<MapPoint, UUID> {
    @Query(value = """
        SELECT
            mp.id AS id,
            mp.point_type AS "pointType",
            ST_Y(mp.location) AS "latitude",
            ST_X(mp.location) AS "longitude",
            rr.emergency_level AS "emergencyLevel",
            hr.hazard_type AS "hazardType",
            CASE
                WHEN mp.point_type = 'SOS' THEN rr.status
                WHEN mp.point_type = 'HAZARD' THEN hr.status
                ELSE NULL
            END AS status
        FROM map_points mp
        LEFT JOIN rescue_requests rr
             ON rr.id = mp.id AND mp.point_type = 'SOS'
         LEFT JOIN hazard_reports hr
             ON hr.id = mp.id AND mp.point_type = 'HAZARD'
         LEFT JOIN safe_points sp
             ON sp.id = mp.id AND mp.point_type = 'SAFE_ZONE'
         LEFT JOIN warehouses wh
             ON wh.id = mp.id AND mp.point_type = 'WARE_HOUSE'
        """, nativeQuery = true)
    List<MapPointProjection> findMapPoints();
}
