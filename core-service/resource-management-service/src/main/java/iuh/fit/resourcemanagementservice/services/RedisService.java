package iuh.fit.resourcemanagementservice.services;

import iuh.fit.resourcemanagementservice.dtos.redis.TeamLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final ObjectMapper objectMapper;

    public void saveLocation(
            UUID campaignTeamId,
            TeamLocation location) {
        String key = TEAM_LOCATION_KEY + campaignTeamId;

        saveLocationGIS(location.latitude(), location.longitude(), campaignTeamId);

        redisTemplate.opsForValue().set(key, location);
    }


    private void saveLocationGIS(Double latitude, Double longitude, UUID campaignTeamId) {
        String key = TEAM_LOCATION_KEY_GIS + campaignTeamId;
        redisTemplate.opsForGeo().add(key, new org.springframework.data.geo.Point(longitude, latitude), campaignTeamId.toString());
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