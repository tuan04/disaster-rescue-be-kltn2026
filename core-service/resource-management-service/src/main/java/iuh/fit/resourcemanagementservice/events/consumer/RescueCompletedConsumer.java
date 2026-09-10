package iuh.fit.resourcemanagementservice.events.consumer;

import iuh.fit.common.kafka.dto.RescueCompletedEvent;
import iuh.fit.resourcemanagementservice.services.CampaignTeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RescueCompletedConsumer {

    private final CampaignTeamService campaignTeamService;

    @KafkaListener(topics = RescueCompletedEvent.TOPIC_NAME, groupId = "resource-group")
    public void consumeRescueCompletedEvent(RescueCompletedEvent event) {
        log.info("Received RescueCompletedEvent from topic {}: {}", RescueCompletedEvent.TOPIC_NAME, event);
        campaignTeamService.handleRescueCompleted(event.campaignTeamId());
    }
}
