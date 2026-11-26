package iuh.fit.notificationservice.kafka.consumer;

import iuh.fit.common.kafka.dto.RescueAcceptedEvent;
import iuh.fit.common.kafka.dto.RescueCanceledEvent;
import iuh.fit.common.kafka.dto.RescueCompletedEvent;
import iuh.fit.notificationservice.socket.WebSocketNotificationService;
import iuh.fit.notificationservice.services.RescueAssignmentNotificationService;
import iuh.fit.notificationservice.dtos.NotificationSocketMessage;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    public static final String OPERATIONS_TOPIC = "/topic/operations/rescue-notifications";

    private final WebSocketNotificationService webSocketNotificationService;
    private final RescueAssignmentNotificationService rescueAssignmentNotificationService;

    @KafkaHandler
    public void consumeRescueAcceptedEvent(RescueAcceptedEvent event) {
        log.info("Broadcasting RescueAcceptedEvent for request {}: team={}", event.requestId(), event.teamName());
        rescueAssignmentNotificationService.notifyTeamAccepted(event);
        webSocketNotificationService.broadcast("/topic/rescue-requests/" + event.requestId() + "/status", event);
        // Admin và điều phối chỉ nhận thông báo trực tiếp, không tạo UserNotification.
        webSocketNotificationService.broadcast(OPERATIONS_TOPIC, NotificationSocketMessage.builder()
                .id(event.assignmentId())
                .referenceId(event.requestId())
                .type("RESCUE_ACCEPTED")
                .title("Đội cứu hộ đã tiếp nhận nhiệm vụ")
                .content("Đội " + event.teamName() + " đã chấp nhận nhiệm vụ cứu hộ.")
                .createdAt(LocalDateTime.ofInstant(event.acceptedAt(), ZoneOffset.UTC))
                .build());
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
