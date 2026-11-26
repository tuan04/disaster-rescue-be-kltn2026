package iuh.fit.notificationservice.kafka.consumer;

import iuh.fit.common.kafka.dto.RescueRejectedEvent;
import iuh.fit.notificationservice.dtos.NotificationSocketMessage;
import iuh.fit.notificationservice.services.RescueAssignmentNotificationService;
import iuh.fit.notificationservice.socket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class RescueRejectedEventConsumer {
    private final RescueAssignmentNotificationService notificationService;
    private final WebSocketNotificationService webSocketNotificationService;

    @KafkaListener(topics = RescueRejectedEvent.TOPIC_NAME)
    public void consume(RescueRejectedEvent event) {
        notificationService.notifyTeamRejected(event);
        // Admin/điều phối chỉ nhận socket, không lưu thông báo cho những người nhận này.
        webSocketNotificationService.broadcast(RescueEventConsumer.OPERATIONS_TOPIC,
                NotificationSocketMessage.builder()
                        .id(event.assignmentId()).referenceId(event.requestId())
                        .type("RESCUE_REJECTED")
                        .title("Đội cứu hộ đã từ chối nhiệm vụ")
                        .content(RescueAssignmentNotificationService.rejectionContent(event))
                        .createdAt(LocalDateTime.ofInstant(event.rejectedAt(), ZoneOffset.UTC))
                        .build());
        // Một đội từ chối không có nghĩa yêu cầu SOS đã kết thúc:
        // không gửi REJECTED vào kênh trạng thái chung của yêu cầu SOS.
    }
}
