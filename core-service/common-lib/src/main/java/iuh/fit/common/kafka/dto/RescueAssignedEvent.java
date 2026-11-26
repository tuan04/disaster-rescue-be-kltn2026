package iuh.fit.common.kafka.dto;

import java.time.Instant;
import java.util.UUID;

public record RescueAssignedEvent(
        UUID assignmentId,
        UUID requestId,
        UUID teamId,
        String teamName,
        Instant occurredAt
) {
    public static final String TOPIC_NAME = "rescue-assigned-event";
}
