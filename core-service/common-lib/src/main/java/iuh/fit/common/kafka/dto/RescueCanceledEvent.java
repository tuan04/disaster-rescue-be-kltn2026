package iuh.fit.common.kafka.dto;

import java.time.Instant;
import java.util.UUID;

public record RescueCanceledEvent(
        UUID assignmentId,
        UUID requestId,
        UUID campaignTeamId,
        String reason,
        Instant canceledAt) {
    public static final String TOPIC_NAME = "rescue-event";
}
