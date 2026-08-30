package iuh.fit.notificationservice.services;

import iuh.fit.notificationservice.dtos.redis.TeamLocationRedis;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisService {

    private static final String TEAM_LOCATION_KEY = "team:location:";

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveLocation(
            UUID campaignTeamId,
            Double latitude,
            Double longitude
    ) {
        String key = TEAM_LOCATION_KEY + campaignTeamId;

        TeamLocationRedis location = TeamLocationRedis.builder()
                .latitude(latitude)
                .longitude(longitude)
                .recordedAt(Instant.now())
                .build();

        redisTemplate.opsForValue().set(key, location);
    }

    public TeamLocationRedis getCurrentLocation(UUID campaignTeamId) {
        String key = TEAM_LOCATION_KEY + campaignTeamId;

        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        return (TeamLocationRedis) value;
    }

    public void deleteLocation(UUID campaignTeamId) {
        redisTemplate.delete(TEAM_LOCATION_KEY + campaignTeamId);
    }
}