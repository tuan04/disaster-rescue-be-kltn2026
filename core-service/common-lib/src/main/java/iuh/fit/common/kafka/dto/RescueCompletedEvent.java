package iuh.fit.common.kafka.dto;

import java.time.Instant;
import java.util.UUID;

public record RescueCompletedEvent(
        String status,
        UUID assignmentId,
        UUID requestId,
        UUID campaignTeamId,
        Instant completedAt) {
    public static final String TOPIC_NAME = "rescue-event";

    public RescueCompletedEvent(
            UUID assignmentId,
            UUID requestId,
            UUID campaignTeamId,
            Instant completedAt) {
        this("COMPLETED", assignmentId, requestId, campaignTeamId, completedAt);
    }
}
