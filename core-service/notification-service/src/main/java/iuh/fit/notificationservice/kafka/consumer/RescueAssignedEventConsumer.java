package iuh.fit.notificationservice.kafka.consumer;

import iuh.fit.common.kafka.dto.RescueAssignedEvent;
import iuh.fit.notificationservice.services.RescueAssignmentNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RescueAssignedEventConsumer {
    private final RescueAssignmentNotificationService notificationService;

    @KafkaListener(topics = RescueAssignedEvent.TOPIC_NAME)
    public void consume(RescueAssignedEvent event) {
        log.info("Received rescue assignment: assignmentId={}, requestId={}, teamId={}",
                event.assignmentId(), event.requestId(), event.teamId());
        notificationService.notifyTeam(event);
    }
}
