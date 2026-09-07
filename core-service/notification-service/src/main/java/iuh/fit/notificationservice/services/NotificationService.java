package iuh.fit.notificationservice.services;

import iuh.fit.common.kafka.dto.SOSResponse;
import iuh.fit.notificationservice.dtos.NotificationResponse;
import iuh.fit.notificationservice.dtos.PageResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    /**
     * Phân phối thông báo cứu hộ SOS cho các cứu hộ viên xung quanh theo bán kính
     *
     * @param event Thông tin sự kiện SOS nhận từ Kafka
     */
    void processSOSEvent(SOSResponse event);

    /**
     * Lấy danh sách thông báo của user có phân trang (chưa bị xóa)
     *
     * @param userId ID người dùng
     * @param page   Số trang (0-indexed)
     * @param size   Số lượng mỗi trang
     * @return Đối tượng PageResponse chứa danh sách thông báo
     */
    PageResponse<NotificationResponse> getNotificationsByUserId(UUID userId, int page, int size);

    /**
     * Lấy toàn bộ thông báo của user (chưa bị xóa)
     *
     * @param userId ID người dùng
     * @return Danh sách thông báo
     */
    List<NotificationResponse> getNotificationsByUserId(UUID userId);

    /**
     * Đánh dấu 1 thông báo là đã đọc
     *
     * @param userNotificationId ID của UserNotification
     * @param userId             ID người dùng
     */
    void markAsRead(UUID userNotificationId, UUID userId);

    /**
     * Đánh dấu tất cả thông báo của user là đã đọc
     *
     * @param userId ID người dùng
     */
    void markAllAsRead(UUID userId);

    /**
     * Đánh dấu 1 thông báo là đã xóa (soft delete)
     *
     * @param userNotificationId ID của UserNotification
     * @param userId             ID người dùng
     */
    void markAsDeleted(UUID userNotificationId, UUID userId);

    /**
     * Đánh dấu tất cả thông báo của user là đã xóa
     *
     * @param userId ID người dùng
     */
    void markAllAsDeleted(UUID userId);
}
