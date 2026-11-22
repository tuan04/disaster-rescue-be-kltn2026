package iuh.fit.common.kafka.dto;

import java.time.Instant;
import java.util.UUID;

public record MapPointCreatedEvent(
        UUID mapPointId,
        String pointType,
        String name,
        String description,
        String address,
        Double latitude,
        Double longitude,
        UUID locationId,
        Instant occurredAt
) {
    public static final String TOPIC = "map-point-created";
}