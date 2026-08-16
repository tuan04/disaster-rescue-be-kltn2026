package iuh.fit.dispatchservice.repositories;

import iuh.fit.dispatchservice.dtos.request.MapPointFilterRequest;
import iuh.fit.dispatchservice.dtos.response.MapPointRes;
import iuh.fit.dispatchservice.enums.PointType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MapPointSearchRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<MapPointRes> findMapPoints(MapPointFilterRequest filter) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                mp.id,
                mp.point_type,
                ST_Y(mp.location) AS latitude,
                ST_X(mp.location) AS longitude,
                rr.emergency_level,
                CASE
                    WHEN mp.point_type = 'HAZARD' THEN hr.hazard_type
                    WHEN mp.point_type = 'SAFE_ZONE' THEN sp.safe_point_type
                    ELSE NULL
                END AS sub_type,
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
            WHERE mp.is_visible = true
            """);

        MapSqlParameterSource params = new MapSqlParameterSource();

        if (filter.fromTime() != null) {
            sql.append(" AND mp.created_at >= :fromTime");
            params.addValue("fromTime", filter.fromTime());
        }

        if (filter.toTime() != null) {
            sql.append(" AND mp.created_at <= :toTime");
            params.addValue("toTime", filter.toTime());
        }

        if (filter.pointTypes() != null && !filter.pointTypes().isEmpty()) {
            sql.append(" AND mp.point_type IN (:pointTypes)");
            params.addValue("pointTypes", filter.pointTypes().stream().map(Enum::name).toList());
        }

        if (filter.rescueStatuses() != null && !filter.rescueStatuses().isEmpty()) {
            sql.append(" AND (mp.point_type <> 'SOS' OR rr.status IN (:rescueStatuses))");
            params.addValue("rescueStatuses", filter.rescueStatuses().stream().map(Enum::name).toList());
        }

        if (filter.emergencyLevels() != null && !filter.emergencyLevels().isEmpty()) {
            sql.append(" AND (mp.point_type <> 'SOS' OR rr.emergency_level IN (:emergencyLevels))");
            params.addValue("emergencyLevels", filter.emergencyLevels().stream().map(Enum::name).toList());
        }

        if (filter.hazardStatuses() != null && !filter.hazardStatuses().isEmpty()) {
            sql.append(" AND (mp.point_type <> 'HAZARD' OR hr.status IN (:hazardStatuses))");
            params.addValue("hazardStatuses", filter.hazardStatuses().stream().map(Enum::name).toList());
        }

        if (filter.hazardTypes() != null && !filter.hazardTypes().isEmpty()) {
            sql.append(" AND (mp.point_type <> 'HAZARD' OR hr.hazard_type IN (:hazardTypes))");
            params.addValue("hazardTypes", filter.hazardTypes().stream().map(Enum::name).toList());
        }

        if (filter.safePointTypes() != null && !filter.safePointTypes().isEmpty()) {
            sql.append(" AND (mp.point_type <> 'SAFE_ZONE' OR sp.safe_point_type IN (:safePointTypes))");
            params.addValue("safePointTypes", filter.safePointTypes().stream().map(Enum::name).toList());
        }

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            MapPointRes res = new MapPointRes();
            res.setId(rs.getObject("id", UUID.class));
            res.setPointType(PointType.valueOf(rs.getString("point_type")));
            res.setLatitude(rs.getDouble("latitude"));
            res.setLongitude(rs.getDouble("longitude"));
            res.setPriority(rs.getString("emergency_level"));
            res.setSubType(rs.getString("sub_type"));
            res.setStatus(rs.getString("status"));
            return res;
        });
    }
}