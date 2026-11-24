package iuh.fit.common.kafka.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserTeamMembershipSyncEvent(
        UUID userId,
        UUID teamId,
        String teamName,
        String roleInTeam, // "LEADER" hoặc "RESCUER"
        LocalDateTime timestamp
) {
    public static final String TOPIC_NAME = "user-team-sync-events";
}
