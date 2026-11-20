package iuh.fit.notificationservice.kafka.consumer;

import iuh.fit.common.kafka.dto.MapPointCreatedEvent;
import iuh.fit.notificationservice.controller.MapPointNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MapPointEventConsumer {

    private final MapPointNotificationService notificationService;

    @KafkaListener(
            topics = MapPointCreatedEvent.TOPIC,
            groupId = "notification-group"
    )
    public void consume(MapPointCreatedEvent event) {
        notificationService.process(event);
    }
}