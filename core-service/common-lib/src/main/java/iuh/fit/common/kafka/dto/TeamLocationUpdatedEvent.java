package iuh.fit.common.kafka.dto;

import java.time.Instant;
import java.util.UUID;

public record TeamLocationUpdatedEvent(
        UUID teamId,
        Double latitude,
        Double longitude,
        Double speed,
        Double heading,
        Instant recordedAt
) {}
