package iuh.fit.notificationservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.kafka.dto.SOSResponse;
import iuh.fit.notificationservice.dtos.NotificationResponse;
import iuh.fit.notificationservice.dtos.NotificationSocketMessage;
import iuh.fit.notificationservice.dtos.PageResponse;
import iuh.fit.notificationservice.entity.Notification;
import iuh.fit.notificationservice.entity.UserNotification;
import iuh.fit.notificationservice.redis.LocationTrackingService;
import iuh.fit.notificationservice.repositories.NotificationRepository;
import iuh.fit.notificationservice.repositories.UserNotificationRepository;
import iuh.fit.notificationservice.socket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int DEFAULT_RADIUS_METERS = 5000;
    private static final String NOTIFICATION_TYPE_SOS = "SOS_ALERT";

    private final LocationTrackingService locationTrackingService;
    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    @Transactional
    public void sendNotificationToUser(UUID targetUserId, String type, String title, String content, UUID referenceId) {
        if (targetUserId == null) {
            return;
        }

        Notification notification = Notification.builder()
                .referenceId(referenceId)
                .type(type)
                .title(title)
                .content(content)
                .build();
        Notification savedNotification = notificationRepository.save(notification);

        UserNotification userNotification = UserNotification.builder()
                .userId(targetUserId)
                .notification(savedNotification)
                .isRead(false)
                .isDeleted(false)
                .build();
        userNotificationRepository.save(userNotification);

        NotificationSocketMessage socketMessage = NotificationSocketMessage.builder()
                .id(savedNotification.getId())
                .referenceId(referenceId)
                .type(type)
                .title(savedNotification.getTitle())
                .content(savedNotification.getContent())
                .createdAt(savedNotification.getCreatedAt())
                .build();

        webSocketNotificationService.sendToUser(targetUserId, socketMessage);
    }

    @Transactional
    public void processSOSEvent(SOSResponse event) {
        if (event == null) {
            return;
        }

        int radius = (event.radiusMeters() != null && event.radiusMeters() > 0)
                ? event.radiusMeters()
                : DEFAULT_RADIUS_METERS;

        List<String> teamIds = locationTrackingService.findUsersInRadius(
                event.longitude(),
                event.latitude(),
                radius
        );

        System.out.println("Found teamId in radius: " + teamIds);

        if (teamIds.isEmpty()) {
            return;
        }

        List<UUID> validUserIds = new ArrayList<>();
        for (String rawId : teamIds) {
            validUserIds.add(UUID.fromString(rawId));
        }

        if (validUserIds.isEmpty()) {
            return;
        }

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

        List<UserNotification> userNotifications = validUserIds.stream()
                .map(userId -> UserNotification.builder()
                        .userId(userId)
                        .notification(savedNotification)
                        .isRead(false)
                        .isDeleted(false)
                        .build())
                .toList();

        userNotificationRepository.saveAll(userNotifications);

        NotificationSocketMessage socketMessage = NotificationSocketMessage.builder()
                .id(savedNotification.getId())
                .referenceId(savedNotification.getReferenceId())
                .type(savedNotification.getType())
                .title(savedNotification.getTitle())
                .content(savedNotification.getContent())
                .createdAt(savedNotification.getCreatedAt())
                .latitude(event.latitude())
                .longitude(event.longitude())
                .emergencyLevel(event.emergencyLevel() != null ? event.emergencyLevel() : "HIGH")
                .reporterPhone(event.reporterPhone())
                .build();

        webSocketNotificationService.sendToUsers(validUserIds, socketMessage);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotificationsByUserId(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<UserNotification> userNotificationPage = userNotificationRepository
                .findAllByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, pageable);

        List<NotificationResponse> content = userNotificationPage.getContent().stream()
                .map(un -> NotificationResponse.builder()
                        .id(un.getId())
                        .notificationId(un.getNotification().getId())
                        .referenceId(un.getNotification().getReferenceId())
                        .type(un.getNotification().getType())
                        .title(un.getNotification().getTitle())
                        .content(un.getNotification().getContent())
                        .isRead(un.getIsRead())
                        .createdAt(un.getCreatedAt())
                        .build())
                .toList();

        return PageResponse.<NotificationResponse>builder()
                .content(content)
                .page(userNotificationPage.getNumber())
                .size(userNotificationPage.getSize())
                .totalElements(userNotificationPage.getTotalElements())
                .totalPages(userNotificationPage.getTotalPages())
                .isLast(userNotificationPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByUserId(UUID userId) {
        List<UserNotification> userNotifications = userNotificationRepository
                .findAllByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);

        return userNotifications.stream()
                .map(un -> NotificationResponse.builder()
                        .id(un.getId())
                        .notificationId(un.getNotification().getId())
                        .referenceId(un.getNotification().getReferenceId())
                        .type(un.getNotification().getType())
                        .title(un.getNotification().getTitle())
                        .content(un.getNotification().getContent())
                        .isRead(un.getIsRead())
                        .createdAt(un.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void markAsRead(UUID userNotificationId, UUID userId) {
        UserNotification userNotification = userNotificationRepository
                .findByIdAndUserIdAndIsDeletedFalse(userNotificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thông báo"));

        userNotification.setIsRead(true);
        userNotificationRepository.save(userNotification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        userNotificationRepository.markAllAsReadByUserId(userId);
    }

    @Transactional
    public void markAsDeleted(UUID userNotificationId, UUID userId) {
        UserNotification userNotification = userNotificationRepository
                .findByIdAndUserIdAndIsDeletedFalse(userNotificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thông báo"));

        userNotification.setIsDeleted(true);
        userNotificationRepository.save(userNotification);
    }

    @Transactional
    public void markAllAsDeleted(UUID userId) {
        userNotificationRepository.markAllAsDeletedByUserId(userId);
    }
}
