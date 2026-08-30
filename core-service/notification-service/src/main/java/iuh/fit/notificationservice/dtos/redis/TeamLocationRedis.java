package iuh.fit.notificationservice.dtos.redis;

import lombok.Builder;

import java.time.Instant;

@Builder
public record TeamLocationRedis(
        Double latitude,
        Double longitude,
        Instant recordedAt
) {
}
