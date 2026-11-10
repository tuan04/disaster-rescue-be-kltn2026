package iuh.fit.resourcemanagementservice.events.producer;

import iuh.fit.common.kafka.dto.TeamMemberEvent;
import iuh.fit.common.kafka.dto.TeamMemberEventType;
import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class TeamEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    public static final String TEAM_EVENTS_TOPIC = "team-events";

    public void publishTeamMemberEvent(TeamMemberEvent event) {
        log.info("Publishing TeamMemberEvent to topic {}: type={}, team={}, targetUser={}",
                TEAM_EVENTS_TOPIC, event.eventType(), event.teamName(), event.targetUserId());
        kafkaTemplate.send(
                TEAM_EVENTS_TOPIC,
                event.targetUserId().toString(),
                event
        );
    }

    public void sendTeamEvent(
            CampaignTeam team,
            TeamMemberEventType eventType,
            UUID targetUserId,
            UUID actorId,
            String actorName,
            String message
    ) {
        if (targetUserId == null) {
            return;
        }
        publishTeamMemberEvent(new TeamMemberEvent(
                eventType,
                team.getId(),
                team.getTeamName(),
                team.getCampaign().getId(),
                targetUserId,
                actorId,
                actorName,
                message,
                LocalDateTime.now()
        ));
    }

    public void sendTeamEventToUsers(
            CampaignTeam team,
            TeamMemberEventType eventType,
            Collection<UUID> targetUserIds,
            UUID actorId,
            String actorName,
            String message
    ) {
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            return;
        }
        for (UUID targetUserId : targetUserIds) {
            if (!targetUserId.equals(actorId)) {
                sendTeamEvent(team, eventType, targetUserId, actorId, actorName, message);
            }
        }
    }
}
