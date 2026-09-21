package iuh.fit.resourcemanagementservice.services;

import iuh.fit.resourcemanagementservice.dtos.redis.TeamLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisService {

    private static final String TEAM_LOCATION_KEY = "team:location:";
    private static final String TEAM_LOCATION_KEY_GIS = "team:location_gis:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public List<String> findTeamIdRadius(double longitude, double latitude, int radiusMeters) {

        Point centerPoint = new Point(longitude, latitude);
        Distance searchDistance = new Distance(radiusMeters, RedisGeoCommands.DistanceUnit.METERS);
        Circle searchCircle = new Circle(centerPoint, searchDistance);

        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults =
                stringRedisTemplate.opsForGeo().radius(TEAM_LOCATION_KEY_GIS + "all", searchCircle);

        if (geoResults == null || geoResults.getContent().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> teamIds = geoResults.getContent().stream()
                .map(result -> result.getContent().getName())
                .filter(Objects::nonNull)
                .toList();
        return teamIds;
    }

    public void saveLocation(
            UUID campaignTeamId,
            TeamLocation location) {
        String key = TEAM_LOCATION_KEY + campaignTeamId;

        saveLocationGIS(location.latitude(), location.longitude(), campaignTeamId);

        redisTemplate.opsForValue().set(key, location);
    }

    private void saveLocationGIS(Double latitude, Double longitude, UUID campaignTeamId) {
        Point point = new Point(longitude, latitude);
        String teamIdStr = campaignTeamId.toString();

        stringRedisTemplate.opsForGeo().add(TEAM_LOCATION_KEY_GIS + "all", point, teamIdStr);

        String key = TEAM_LOCATION_KEY_GIS + campaignTeamId;
        stringRedisTemplate.opsForGeo().add(key, point, teamIdStr);
    }

    public TeamLocation getCurrentLocation(UUID campaignTeamId) {
        String key = TEAM_LOCATION_KEY + campaignTeamId;

        Object value = redisTemplate.opsForValue().get(key);

        return convertToTeamLocation(value);
    }

    /**
     * Lấy toàn bộ vị trí của tất cả các đội cứu hộ hiện có trên Redis
     */
    public List<TeamLocation> getAllLocations() {
        Set<String> keys = redisTemplate.keys(TEAM_LOCATION_KEY + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<Object> values = redisTemplate.opsForValue().multiGet(keys);
        if (values == null) {
            return Collections.emptyList();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .map(this::convertToTeamLocation)
                .toList();
    }

    private TeamLocation convertToTeamLocation(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof TeamLocation teamLocation) {
            return teamLocation;
        }
        return objectMapper.convertValue(value, TeamLocation.class);
    }
}