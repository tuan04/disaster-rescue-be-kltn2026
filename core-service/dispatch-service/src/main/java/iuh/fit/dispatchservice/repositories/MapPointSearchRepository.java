package iuh.fit.dispatchservice.repositories;

import iuh.fit.dispatchservice.dtos.request.MapPointFilterRequest;
import iuh.fit.dispatchservice.dtos.request.StrategicPointsFilter;
import iuh.fit.dispatchservice.dtos.response.*;
import iuh.fit.dispatchservice.enums.HazardStatus;
import iuh.fit.dispatchservice.enums.HazardType;
import iuh.fit.dispatchservice.enums.PointType;
import iuh.fit.dispatchservice.enums.SafePointType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
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

    public Page<MapPointDetailResponse> findStrategicPoints(StrategicPointsFilter filter, Pageable pageable) {
        StringBuilder selectSql = new StringBuilder("""
            SELECT
                mp.id,
                mp.point_type,
                ST_Y(mp.location) AS latitude,
                ST_X(mp.location) AS longitude,
                mp.address,
                mp.created_at,
                hr.hazard_type,
                hr.description AS hazard_description,
                hr.image_urls,
                hr.status AS hazard_status,
                sp.name AS safe_point_name,
                sp.contact_phone AS safe_point_phone,
                sp.safe_point_type,
                sp.is_active AS safe_point_is_active,
                wh.name AS warehouse_name,
                wh.manager_phone,
                wh.is_active AS warehouse_is_active
            """);

        StringBuilder fromAndWhereSql = new StringBuilder("""
            FROM map_points mp
            LEFT JOIN hazard_reports hr
                ON hr.id = mp.id AND mp.point_type = 'HAZARD'
            LEFT JOIN safe_points sp
                ON sp.id = mp.id AND mp.point_type = 'SAFE_ZONE'
            LEFT JOIN warehouses wh
                ON wh.id = mp.id AND mp.point_type = 'WARE_HOUSE'
            WHERE mp.is_visible = true
              AND mp.point_type <> 'SOS'
            """);

        MapSqlParameterSource params = new MapSqlParameterSource();

        if (filter != null) {
            if (filter.pointType() != null && !filter.pointType().isBlank()) {
                String pt = filter.pointType().trim().toUpperCase();
                if (!"SOS".equals(pt)) {
                    fromAndWhereSql.append(" AND mp.point_type = :pointType");
                    params.addValue("pointType", pt);
                }
            }

            if (filter.locationId() != null) {
                fromAndWhereSql.append(" AND mp.location_id = :locationId");
                params.addValue("locationId", filter.locationId());
            }

            if (filter.hazardType() != null) {
                fromAndWhereSql.append(" AND (mp.point_type <> 'HAZARD' OR hr.hazard_type = :hazardType)");
                params.addValue("hazardType", filter.hazardType().name());
            }

            if (filter.safePointType() != null) {
                fromAndWhereSql.append(" AND (mp.point_type <> 'SAFE_ZONE' OR sp.safe_point_type = :safePointType)");
                params.addValue("safePointType", filter.safePointType().name());
            }

            if (filter.fromTime() != null) {
                fromAndWhereSql.append(" AND mp.created_at >= :fromTime");
                params.addValue("fromTime", filter.fromTime());
            }

            if (filter.toTime() != null) {
                fromAndWhereSql.append(" AND mp.created_at <= :toTime");
                params.addValue("toTime", filter.toTime());
            }
        }

        // Total count query
        String countSql = "SELECT count(mp.id) " + fromAndWhereSql;
        Long total = jdbcTemplate.queryForObject(countSql, params, Long.class);

        // Data query with pagination
        StringBuilder dataSql = new StringBuilder(selectSql).append(fromAndWhereSql);
        dataSql.append(" ORDER BY mp.created_at DESC");

        if (pageable != null && pageable.isPaged()) {
            dataSql.append(" LIMIT :limit OFFSET :offset");
            params.addValue("limit", pageable.getPageSize());
            params.addValue("offset", pageable.getOffset());
        }

        List<MapPointDetailResponse> content = jdbcTemplate.query(dataSql.toString(), params, (rs, rowNum) -> {
            UUID id = rs.getObject("id", UUID.class);
            PointType pointType = PointType.valueOf(rs.getString("point_type"));
            double latitude = rs.getDouble("latitude");
            double longitude = rs.getDouble("longitude");
            String address = rs.getString("address");
            Timestamp createdAtTs = rs.getTimestamp("created_at");
            LocalDateTime createdAt = createdAtTs != null ? createdAtTs.toLocalDateTime() : null;

            MapPointDetailResInterface detail = switch (pointType) {
                case HAZARD -> {
                    String ht = rs.getString("hazard_type");
                    String hs = rs.getString("hazard_status");
                    Array imgArray = rs.getArray("image_urls");
                    List<String> images = null;
                    if (imgArray != null) {
                        String[] arr = (String[]) imgArray.getArray();
                        images = arr != null ? Arrays.asList(arr) : null;
                    }
                    yield new HazardDetailResponse(
                            id,
                            ht != null ? HazardType.valueOf(ht) : null,
                            rs.getString("hazard_description"),
                            images,
                            hs != null ? HazardStatus.valueOf(hs) : null
                    );
                }
                case SAFE_ZONE -> {
                    String spt = rs.getString("safe_point_type");
                    Boolean isActive = rs.getObject("safe_point_is_active", Boolean.class);
                    yield new SafePointDetailResponse(
                            id,
                            rs.getString("safe_point_name"),
                            rs.getString("safe_point_phone"),
                            spt != null ? SafePointType.valueOf(spt) : null,
                            isActive
                    );
                }
                case WARE_HOUSE -> {
                    Boolean isActive = rs.getObject("warehouse_is_active", Boolean.class);
                    yield new WarehouseDetailResponse(
                            id,
                            rs.getString("warehouse_name"),
                            rs.getString("manager_phone"),
                            isActive
                    );
                }
                case SOS -> null;
            };

            return new MapPointDetailResponse(
                    id,
                    pointType,
                    latitude,
                    longitude,
                    address,
                    createdAt,
                    detail
            );
        });

        return new PageImpl<>(content, pageable != null ? pageable : Pageable.unpaged(), total != null ? total : 0L);
    }
}