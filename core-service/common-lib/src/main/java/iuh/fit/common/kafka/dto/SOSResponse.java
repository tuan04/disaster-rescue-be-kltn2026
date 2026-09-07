package iuh.fit.common.kafka.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record SOSResponse(
        UUID id,
        String reporterPhone,
        String emergencyLevel,
        String content,
        String status,
        String source,
        double latitude,
        double longitude,
        Integer radiusMeters
) {
}
