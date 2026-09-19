package iuh.fit.common.kafka.dto;

import java.time.Instant;
import java.util.UUID;

public record RescueCanceledEvent(
        String status,
        UUID assignmentId,
        UUID requestId,
        UUID campaignTeamId,
        String reason,
        Instant canceledAt) {
    public static final String TOPIC_NAME = "rescue-event";

    public RescueCanceledEvent(
            UUID assignmentId,
            UUID requestId,
            UUID campaignTeamId,
            String reason,
            Instant canceledAt) {
        this("CANCELED", assignmentId, requestId, campaignTeamId, reason, canceledAt);
    }
}
