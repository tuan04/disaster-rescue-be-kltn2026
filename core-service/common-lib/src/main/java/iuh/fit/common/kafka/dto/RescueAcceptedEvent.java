package iuh.fit.common.kafka.dto;

import java.time.Instant;
import java.util.UUID;

public record RescueAcceptedEvent(
        String status,
        UUID assignmentId,
        UUID requestId,
        UUID teamId,
        String teamName,
        String leaderPhone,
        Instant acceptedAt) {
    public static final String TOPIC_NAME = "rescue-event";

    public RescueAcceptedEvent(
            UUID assignmentId,
            UUID requestId,
            UUID teamId,
            String teamName,
            String leaderPhone,
            Instant acceptedAt) {
        this("ACCEPTED", assignmentId, requestId, teamId, teamName, leaderPhone, acceptedAt);
    }
}
