package iuh.fit.common.kafka.dto;

import java.time.Instant;
import java.util.UUID;

public record RescueRejectedEvent(
        UUID assignmentId,
        UUID requestId,
        UUID teamId,
        String teamName,
        String reason,
        Instant rejectedAt
) {
    public static final String TOPIC_NAME = "rescue-rejected-event";
}
