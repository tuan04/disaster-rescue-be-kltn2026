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
import org.springframework.data.redis.core.script.DefaultRedisScript;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisService {

    public static final String RESCUER_MEMBERSHIP_LOCK_KEY = "lock:rescuer:membership:";

    private static final String TEAM_LOCATION_KEY = "team:location:";
    private static final String TEAM_LOCATION_KEY_GIS = "team:location_gis:";
    private static final Duration TEAM_LOCATION_TTL = Duration.ofMinutes(30);

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

        redisTemplate.opsForValue().set(key, location, TEAM_LOCATION_TTL);
    }

    private void saveLocationGIS(Double latitude, Double longitude, UUID campaignTeamId) {
        Point point = new Point(longitude, latitude);
        String teamIdStr = campaignTeamId.toString();

        String key = TEAM_LOCATION_KEY_GIS + "all";

        stringRedisTemplate.opsForGeo().add(key, point, teamIdStr);
        stringRedisTemplate.expire(key, TEAM_LOCATION_TTL);
    }

    public TeamLocation getCurrentLocation(UUID campaignTeamId) {
        String key = TEAM_LOCATION_KEY + campaignTeamId;

        Object value = redisTemplate.opsForValue().get(key);

        return withTeamId(convertToTeamLocation(value), campaignTeamId.toString());
    }

    /**
     * Lấy toàn bộ vị trí của tất cả các đội cứu hộ hiện có trên Redis
     */
    public List<TeamLocation> getAllLocations() {
        Set<String> keys = redisTemplate.keys(TEAM_LOCATION_KEY + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> orderedKeys = List.copyOf(keys);
        List<Object> values = redisTemplate.opsForValue().multiGet(orderedKeys);
        if (values == null) {
            return Collections.emptyList();
        }

        return java.util.stream.IntStream.range(0, values.size())
                .filter(index -> values.get(index) != null)
                .mapToObj(index -> withTeamId(convertToTeamLocation(values.get(index)),
                        orderedKeys.get(index).substring(TEAM_LOCATION_KEY.length())))
                .toList();
    }

    // Older Redis snapshots predate teamId; recover it from their stable key.
    private TeamLocation withTeamId(TeamLocation location, String teamId) {
        return location == null ? null : location.toBuilder().teamId(teamId).build();
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

    public void deleteLocation(UUID campaignTeamId) {
        if (campaignTeamId == null) {
            return;
        }
        String teamIdStr = campaignTeamId.toString();
        redisTemplate.delete(TEAM_LOCATION_KEY + campaignTeamId);
        stringRedisTemplate.delete(TEAM_LOCATION_KEY_GIS + campaignTeamId);
        stringRedisTemplate.opsForGeo().remove(TEAM_LOCATION_KEY_GIS + "all", teamIdStr);
    }

    /**
     * Lấy khóa phân tán Redis bằng lệnh nguyên tử SETNX kèm thời gian hết hạn (TTL)
     */
    public String acquireLock(String key, long timeoutSeconds) {
        String token = UUID.randomUUID().toString();
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, token, Duration.ofSeconds(timeoutSeconds));
        return Boolean.TRUE.equals(success) ? token : null;
    }

    /**
     * Giải phóng khóa phân tán Redis
     */
    private static final String RELEASE_LOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";

    public void releaseLock(String key, String token) {
        if (key == null || token == null) {
            return;
        }
        stringRedisTemplate.execute(
                new DefaultRedisScript<>(RELEASE_LOCK_SCRIPT, Long.class),
                Collections.singletonList(key),
                token
        );
    }
}