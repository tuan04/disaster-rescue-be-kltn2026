package iuh.fit.dispatchservice.kafka.producer;

import iuh.fit.common.kafka.dto.RescueCanceledEvent;
import iuh.fit.common.kafka.dto.RescueCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RescueEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishRescueCompletedEvent(RescueCompletedEvent event) {
        log.info("Publishing RescueCompletedEvent to topic {}: {}", RescueCompletedEvent.TOPIC_NAME, event);
        kafkaTemplate.send(
                RescueCompletedEvent.TOPIC_NAME,
                event.campaignTeamId().toString(),
                event);
    }

    public void publishRescueCanceledEvent(RescueCanceledEvent event) {
        log.info("Publishing RescueCanceledEvent to topic {}: {}", RescueCanceledEvent.TOPIC_NAME, event);
        kafkaTemplate.send(
                RescueCanceledEvent.TOPIC_NAME,
                event.campaignTeamId().toString(),
                event);
    }
}
