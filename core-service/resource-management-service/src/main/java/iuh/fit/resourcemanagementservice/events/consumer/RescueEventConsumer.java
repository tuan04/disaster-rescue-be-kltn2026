package iuh.fit.resourcemanagementservice.events.consumer;

import iuh.fit.common.kafka.dto.RescueCanceledEvent;
import iuh.fit.common.kafka.dto.RescueCompletedEvent;
import iuh.fit.resourcemanagementservice.services.CampaignTeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@KafkaListener(topics = RescueCompletedEvent.TOPIC_NAME)
@RequiredArgsConstructor
public class RescueEventConsumer {

    private final CampaignTeamService campaignTeamService;

    @KafkaHandler
    public void consumeRescueCompletedEvent(RescueCompletedEvent event) {
        log.info("Received RescueCompletedEvent from topic {}: {}", RescueCompletedEvent.TOPIC_NAME, event);
        campaignTeamService.setTeamReady(event.campaignTeamId());
    }

    @KafkaHandler
    public void consumeRescueCanceledEvent(RescueCanceledEvent event) {
        log.info("Received RescueCanceledEvent from topic {}: {}", RescueCanceledEvent.TOPIC_NAME, event);
        campaignTeamService.setTeamReady(event.campaignTeamId());
    }
}
