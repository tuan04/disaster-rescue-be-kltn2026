package iuh.fit.notificationservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.kafka.dto.SOSResponse;
import iuh.fit.notificationservice.dtos.NotificationResponse;
import iuh.fit.notificationservice.dtos.NotificationSocketMessage;
import iuh.fit.common.response.PageResponse;
import iuh.fit.notificationservice.entity.Notification;
import iuh.fit.notificationservice.entity.UserNotification;
import iuh.fit.notificationservice.repositories.NotificationRepository;
import iuh.fit.notificationservice.repositories.UserNotificationRepository;
import iuh.fit.notificationservice.socket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {


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

    /** Live map alerts are ephemeral; only the rescue request itself is persisted. */
    public void processSOSEvent(SOSResponse event) {
        if (event == null || event.id() == null) return;

        boolean created = "CREATED".equals(event.eventType());
        NotificationSocketMessage message = NotificationSocketMessage.builder()
                .id(event.id())
                .referenceId(event.id())
                .type(created ? "SOS_ALERT" : "SOS_UPDATED")
                .title(created ? "Có yêu cầu cứu hộ SOS mới" : "Yêu cầu cứu hộ đã cập nhật")
                .createdAt(java.time.LocalDateTime.now())
                .latitude(event.latitude())
                .longitude(event.longitude())
                .emergencyLevel(event.emergencyLevel())
                .build();
        // Public map channel: do not broadcast phone numbers or free-form personal content.
        webSocketNotificationService.broadcast("/topic/map/sos", message);
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
