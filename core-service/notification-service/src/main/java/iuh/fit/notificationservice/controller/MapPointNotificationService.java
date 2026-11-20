package iuh.fit.notificationservice.controller;


import iuh.fit.common.kafka.dto.MapPointCreatedEvent;
import iuh.fit.notificationservice.dtos.NotificationSocketMessage;
import iuh.fit.notificationservice.entity.Notification;
import iuh.fit.notificationservice.entity.UserNotification;
import iuh.fit.notificationservice.redis.AllUserRecipientService;
import iuh.fit.notificationservice.repositories.NotificationRepository;
import iuh.fit.notificationservice.repositories.UserNotificationRepository;
import iuh.fit.notificationservice.socket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class MapPointNotificationService {

    private final AllUserRecipientService allUserRecipientService;
    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final WebSocketNotificationService socketService;
    private final TransactionTemplate transactionTemplate;


    public void process(MapPointCreatedEvent event) {
        String title = buildTitle(event.pointType());
        String content = buildContent(event);

        // 1. Lấy người nhận trước khi mở transaction ghi DB.
        var userIds = allUserRecipientService.findAllUserIds();

        if (userIds.isEmpty()) {
            return;
        }

        // 2. Lưu thông báo và người nhận trong cùng transaction.
        NotificationSocketMessage message =
                transactionTemplate.execute(status -> {

                    Notification notification =
                            notificationRepository.save(
                                    Notification.builder()
                                            .referenceId(event.mapPointId())
                                            .type("MAP_POINT_CREATED")
                                            .title(title)
                                            .content(content)
                                            .build()
                            );

                    var recipients = userIds.stream()
                            .map(userId -> UserNotification.builder()
                                    .userId(userId)
                                    .notification(notification)
                                    .isRead(false)
                                    .isDeleted(false)
                                    .build())
                            .toList();

                    userNotificationRepository.saveAll(recipients);

                    return NotificationSocketMessage.builder()
                            .id(notification.getId())
                            .referenceId(event.mapPointId())
                            .type(notification.getType())
                            .title(notification.getTitle())
                            .content(notification.getContent())
                            .createdAt(notification.getCreatedAt())
                            .latitude(event.latitude())
                            .longitude(event.longitude())
                            .build();
                });

        // 3. Transaction đã commit thành công mới gửi socket.
        if (message != null) {
            socketService.sendToUsers(userIds, message);
        }
    }

    private String buildTitle(String pointType) {
        return switch (pointType) {
            case "HAZARD" ->
                    "Cảnh báo: Có điểm nguy hiểm mới";

            case "SAFE_ZONE" ->
                    "Có điểm an toàn mới";

            case "WARE_HOUSE" ->
                    "Có kho cứu trợ mới";

            default -> throw new IllegalArgumentException(
                    "Loại điểm chưa hỗ trợ thông báo: " + pointType
            );
        };
    }

    private String buildContent(MapPointCreatedEvent event) {
        String content;

        if (hasText(event.description())) {
            content = event.description();
        } else if (hasText(event.name())) {
            content = event.name();
        } else {
            content = "Một điểm mới vừa được cập nhật trên bản đồ.";
        }

        if (hasText(event.address())) {
            content += "\nĐịa chỉ: " + event.address();
        }

        return content;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
