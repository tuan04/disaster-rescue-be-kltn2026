package iuh.fit.resourcemanagementservice.dtos.redis;

import lombok.Builder;

import java.time.Instant;

@Builder(toBuilder = true)
public record TeamLocation(
        String teamId,
        Double latitude,
        Double longitude,
        Double speed,
        Double heading,
        Instant recordedAt,
        String teamName,
        String leaderPhone,
        String leaderId
) {
}
