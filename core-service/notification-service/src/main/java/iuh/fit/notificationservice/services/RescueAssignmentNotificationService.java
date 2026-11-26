package iuh.fit.notificationservice.services;

import iuh.fit.common.kafka.dto.RescueAssignedEvent;
import iuh.fit.common.kafka.dto.RescueAcceptedEvent;
import iuh.fit.common.kafka.dto.RescueRejectedEvent;
import iuh.fit.notificationservice.client.ResourceTeamGrpcClient;
import iuh.fit.notificationservice.dtos.NotificationSocketMessage;
import iuh.fit.notificationservice.entity.Notification;
import iuh.fit.notificationservice.entity.UserNotification;
import iuh.fit.notificationservice.repositories.NotificationRepository;
import iuh.fit.notificationservice.repositories.UserNotificationRepository;
import iuh.fit.notificationservice.socket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RescueAssignmentNotificationService {
    private final ResourceTeamGrpcClient resourceTeamGrpcClient;
    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    @Transactional
    public void notifyTeam(RescueAssignedEvent event) {
        saveTeamNotification(event.teamId(), event.requestId(), "RESCUE_ASSIGNED",
                "Nhiệm vụ cứu hộ mới",
                "Đội " + event.teamName() + " được phân công nhiệm vụ cứu hộ mới. Vui lòng kiểm tra nhiệm vụ.");
    }

    @Transactional
    public void notifyTeamAccepted(RescueAcceptedEvent event) {
        saveTeamNotification(event.teamId(), event.requestId(), "RESCUE_ACCEPTED",
                "Đội đã tiếp nhận nhiệm vụ cứu hộ",
                "Đội " + event.teamName() + " đã chấp nhận nhiệm vụ cứu hộ. Các thành viên vui lòng chuẩn bị thực hiện nhiệm vụ.");
    }

    @Transactional
    public void notifyTeamRejected(RescueRejectedEvent event) {
        saveTeamNotification(event.teamId(), event.requestId(), "RESCUE_REJECTED",
                "Đội đã từ chối nhiệm vụ cứu hộ", rejectionContent(event));
    }

    public static String rejectionContent(RescueRejectedEvent event) {
        String content = "Đội " + event.teamName() + " đã từ chối nhiệm vụ cứu hộ.";
        return event.reason() == null || event.reason().isBlank()
                ? content : content + " Lý do: " + event.reason().trim();
    }

    private void saveTeamNotification(UUID teamId, UUID requestId, String type, String title, String content) {
        var recipients = resourceTeamGrpcClient.findRecipientsByTeamIds(List.of(teamId))
                .get(teamId);
        if (!recipients.teamExists() || recipients.userIds().isEmpty()) {
            log.warn("No recipients for rescue request {}, team {}", requestId, teamId);
            return;
        }

        Notification notification = notificationRepository.save(Notification.builder()
                .referenceId(requestId)
                .type(type)
                .title(title)
                .content(content)
                .build());

        var userIds = recipients.userIds().stream().distinct().toList();

        userNotificationRepository.saveAll(userIds.stream()
                .map(userId -> UserNotification.builder()
                        .userId(userId).notification(notification).isRead(false).isDeleted(false).build())
                .toList());

        NotificationSocketMessage message = NotificationSocketMessage.builder()
                .id(notification.getId()).referenceId(requestId).type(notification.getType())
                .title(notification.getTitle()).content(notification.getContent())
                .createdAt(notification.getCreatedAt()).build();

        // Chỉ gửi socket sau khi thông báo và danh sách người nhận đã được commit.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (var userId : userIds) {
                    try {
                        webSocketNotificationService.sendToUser(userId, message);
                    } catch (RuntimeException exception) {
                        // Thông báo vẫn nằm trong DB để client lấy lại khi kết nối.
                        log.error("Failed to send assignment notification to user {}", userId, exception);
                    }
                }
            }
        });
    }
}
