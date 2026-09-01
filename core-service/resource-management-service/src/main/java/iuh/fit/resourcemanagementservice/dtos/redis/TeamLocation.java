package iuh.fit.resourcemanagementservice.dtos.redis;

import lombok.Builder;

import java.time.Instant;

@Builder
public record TeamLocation(
        Double latitude,
        Double longitude,
        Double speed,
        Double heading,
        Instant recordedAt
) {
}
