package iuh.fit.resourcemanagementservice.services;

import iuh.fit.resourcemanagementservice.dtos.redis.TeamLocation;
import iuh.fit.resourcemanagementservice.dtos.request.TeamLocationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisService {

    private static final String TEAM_LOCATION_KEY = "team:location:";
    private static final String TEAM_LOCATION_KEY_GIS = "team:location_gis:";

    private final RedisTemplate<String, Object> redisTemplate;

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

        if (value == null) {
            return null;
        }

        return (TeamLocation) value;
    }

    public void deleteLocation(UUID campaignTeamId) {
        redisTemplate.delete(TEAM_LOCATION_KEY + campaignTeamId);
    }
}