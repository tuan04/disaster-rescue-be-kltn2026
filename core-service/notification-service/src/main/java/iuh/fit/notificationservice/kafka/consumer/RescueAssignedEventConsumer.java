package iuh.fit.notificationservice.kafka.consumer;

import iuh.fit.common.kafka.dto.RescueAssignedEvent;
import iuh.fit.notificationservice.services.RescueAssignmentNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RescueAssignedEventConsumer {
    private final RescueAssignmentNotificationService notificationService;

    @KafkaListener(topics = RescueAssignedEvent.TOPIC_NAME)
    public void consume(RescueAssignedEvent event) {
        notificationService.notifyTeam(event);
    }
}
