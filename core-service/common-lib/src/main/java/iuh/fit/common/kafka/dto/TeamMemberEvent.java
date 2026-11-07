package iuh.fit.common.kafka.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TeamMemberEvent(
        TeamMemberEventType eventType,
        UUID teamId,
        String teamName,
        UUID campaignId,
        UUID targetUserId,
        UUID actorId,
        String actorName,
        String message,
        LocalDateTime timestamp
) {
}

