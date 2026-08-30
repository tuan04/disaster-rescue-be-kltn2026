package iuh.fit.notificationservice.service.impl;

import iuh.fit.notificationservice.dtos.NotificationSocketMessage;
import iuh.fit.notificationservice.dtos.SOSResponse;
import iuh.fit.notificationservice.entity.Notification;
import iuh.fit.notificationservice.entity.UserNotification;
import iuh.fit.notificationservice.redis.LocationTrackingService;
import iuh.fit.notificationservice.repository.NotificationRepository;
import iuh.fit.notificationservice.repository.UserNotificationRepository;
import iuh.fit.notificationservice.service.NotificationService;
import iuh.fit.notificationservice.socket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int DEFAULT_RADIUS_METERS = 5000;
    private static final String NOTIFICATION_TYPE_SOS = "SOS_ALERT";

    private final LocationTrackingService locationTrackingService;
    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    @Override
    @Transactional
    public void processSOSEvent(SOSResponse event) {
        if (event == null) {
            return;
        }

        // Lấy bán kính tìm kiếm (mặc định 5000m nếu event không chỉ định)
        int radius = (event.radiusMeters() != null && event.radiusMeters() > 0)
                ? event.radiusMeters()
                : DEFAULT_RADIUS_METERS;

        // Bước 1: Tìm danh sách userId trong bán kính thảm họa từ Redis GEO
        List<String> rawUserIds = locationTrackingService.findUsersInRadius(
                event.longitude(),
                event.latitude(),
                radius
        );

        if (rawUserIds.isEmpty()) {
            return;
        }

        // Bước 2: Parse danh sách String sang UUID và bắt Exception nếu chuỗi UUID không hợp lệ
        List<UUID> validUserIds = new ArrayList<>();
        for (String rawId : rawUserIds) {
            validUserIds.add(UUID.fromString(rawId));
        }

        if (validUserIds.isEmpty()) {
            return;
        }

        // Bước 3: Tạo và lưu 1 đối tượng Notification vào Database
        String title = String.format("Có người cần cứu hộ khẩn cấp cách bạn trong phạm vi %dm!", radius);
        String content = (event.content() != null && !event.content().isBlank())
                ? event.content()
                : "Yêu cầu cứu hộ khẩn cấp tại tọa độ [" + event.latitude() + ", " + event.longitude() + "]";

        Notification notification = Notification.builder()
                .referenceId(event.id())
                .type(NOTIFICATION_TYPE_SOS)
                .title(title)
                .content(content)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        // Bước 4: Tạo danh sách UserNotification tương ứng cho từng userId
        List<UserNotification> userNotifications = validUserIds.stream()
                .map(userId -> UserNotification.builder()
                        .userId(userId)
                        .notification(savedNotification)
                        .isRead(false)
                        .build())
                .toList();

        // Bước 5: Lưu toàn bộ danh sách UserNotification xuống Database (Batch Insert)
        userNotificationRepository.saveAll(userNotifications);

        // Bước 6: Gửi thông báo thời gian thực qua WebSocket đến tất cả user trong danh sách
        NotificationSocketMessage socketMessage = NotificationSocketMessage.builder()
                .id(savedNotification.getId())
                .referenceId(savedNotification.getReferenceId())
                .type(savedNotification.getType())
                .title(savedNotification.getTitle())
                .content(savedNotification.getContent())
                .createdAt(savedNotification.getCreatedAt())
                .latitude(event.latitude())
                .longitude(event.longitude())
                .emergencyLevel(event.emergencyLevel() != null ? event.emergencyLevel().name() : "HIGH")
                .reporterPhone(event.reporterPhone())
                .build();

        webSocketNotificationService.sendToUsers(validUserIds, socketMessage);
    }
}
