package iuh.fit.notificationservice.kafka.consumer;

import iuh.fit.common.kafka.dto.TeamApprovalEvent;
import iuh.fit.notificationservice.dtos.NotificationSocketMessage;
import iuh.fit.notificationservice.socket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(topics = "team-events")
public class TeamApprovalConsumer {

    private final WebSocketNotificationService webSocketNotificationService;

    @KafkaHandler
    public void consumeTeamApprovalEvent(TeamApprovalEvent event) {
        log.info("Received TeamApprovalEvent from topic 'team-events' for leader {}: team={}, status={}",
                event.leaderId(), event.teamName(), event.status());

        boolean isApproved = "APPROVED".equalsIgnoreCase(event.status());

        String title = isApproved
                ? "Đội cứu hộ đã được phê duyệt!"
                : "Yêu cầu tạo đội bị từ chối";

        String content = isApproved
                ? "Đội cứu hộ \"" + event.teamName() + "\" của bạn đã được Ban quản lý phê duyệt hoạt động."
                : "Yêu cầu thành lập đội \"" + event.teamName() + "\" đã bị từ chối. Lý do: "
                        + (event.rejectionReason() != null && !event.rejectionReason().isBlank()
                                ? event.rejectionReason()
                                : "Không có lý do cụ thể");

        NotificationSocketMessage socketMessage = NotificationSocketMessage.builder()
                .id(UUID.randomUUID())
                .referenceId(event.teamId())
                .type("TEAM_" + event.status())
                .title(title)
                .content(content)
                .createdAt(event.timestamp() != null ? event.timestamp() : LocalDateTime.now())
                .build();

        webSocketNotificationService.sendToUser(event.leaderId(), socketMessage);
    }

    @KafkaHandler(isDefault = true)
    public void handleOtherEvents(Object object) {
        log.debug("Ignored unhandled message from topic team-events: {}", object);
    }
}
