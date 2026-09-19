package iuh.fit.notificationservice.kafka.consumer;

import iuh.fit.common.kafka.dto.RescueAcceptedEvent;
import iuh.fit.common.kafka.dto.RescueCanceledEvent;
import iuh.fit.common.kafka.dto.RescueCompletedEvent;
import iuh.fit.notificationservice.socket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(topics = RescueAcceptedEvent.TOPIC_NAME)
public class RescueEventConsumer {

    private final WebSocketNotificationService webSocketNotificationService;

    @KafkaHandler
    public void consumeRescueAcceptedEvent(RescueAcceptedEvent event) {
        log.info("Broadcasting RescueAcceptedEvent for request {}: team={}", event.requestId(), event.teamName());
        webSocketNotificationService.broadcast("/topic/rescue-requests/" + event.requestId() + "/status", event);
    }

    @KafkaHandler
    public void consumeRescueCompletedEvent(RescueCompletedEvent event) {
        log.info("Broadcasting RescueCompletedEvent for request: {}", event.requestId());
        webSocketNotificationService.broadcast("/topic/rescue-requests/" + event.requestId() + "/status", event);
    }

    @KafkaHandler
    public void consumeRescueCanceledEvent(RescueCanceledEvent event) {
        log.info("Broadcasting RescueCanceledEvent for request: {}", event.requestId());
        webSocketNotificationService.broadcast("/topic/rescue-requests/" + event.requestId() + "/status", event);
    }

    @KafkaHandler(isDefault = true)
    public void handleOtherEvents(Object object) {
        log.debug("Ignored unhandled message from topic {}: {}", RescueAcceptedEvent.TOPIC_NAME, object);
    }
}
