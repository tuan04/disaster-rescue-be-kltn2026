package iuh.fit.notificationservice.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LocationTrackingService {

    public static final String ACTIVE_USERS_LOCATION_KEY = "active_users:locations";
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Tìm danh sách userId (member) nằm trong bán kính thảm họa từ Redis GEO.
     * Sử dụng thuật toán tìm kiếm không gian tích hợp sẵn của Redis GEO (opsForGeo().radius()).
     *
     * @param longitude Kinh độ (x) của tâm vòng tròn
     * @param latitude  Vĩ độ (y) của tâm vòng tròn
     * @param radiusMeters Bán kính quét tính theo mét (Meters)
     * @return Danh sách userId (String của UUID) nằm trong bán kính
     */
    public List<String> findUsersInRadius(double longitude, double latitude, int radiusMeters) {
            // Đảm bảo thứ tự tham số của Point: (Kinh độ - x, Vĩ độ - y)
            Point centerPoint = new Point(longitude, latitude);
            Distance searchDistance = new Distance(radiusMeters, Metrics.valueOf("METERS"));
            Circle searchCircle = new Circle(centerPoint, searchDistance);

            // Gọi lệnh GEORADIUS / GEOSEARCH của Redis
            GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults =
                    stringRedisTemplate.opsForGeo().radius(ACTIVE_USERS_LOCATION_KEY, searchCircle);

            if (geoResults == null || geoResults.getContent().isEmpty()) {
                return Collections.emptyList();
            }

            // Trích xuất member (userId) từ kết quả Redis GEO
            List<String> userIds = geoResults.getContent().stream()
                    .map(result -> result.getContent().getName())
                    .filter(Objects::nonNull)
                    .toList();
            return userIds;
    }
}
