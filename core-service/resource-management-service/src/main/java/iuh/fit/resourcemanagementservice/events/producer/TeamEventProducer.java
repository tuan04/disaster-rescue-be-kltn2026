package iuh.fit.resourcemanagementservice.events.producer;

import iuh.fit.common.kafka.dto.TeamApprovalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class TeamEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    public static final String TEAM_EVENTS_TOPIC = "team-events";

    public void publishTeamApprovalEvent(TeamApprovalEvent event) {
        log.info("Publishing TeamApprovalEvent to topic {}: team={}, leader={}, status={}",
                TEAM_EVENTS_TOPIC, event.teamName(), event.leaderId(), event.status());
        kafkaTemplate.send(
                TEAM_EVENTS_TOPIC,
                event.leaderId().toString(),
                event
        );
    }
}
