package iuh.fit.notificationservice.socket;

import iuh.fit.notificationservice.dtos.NotificationSocketMessage;

import java.util.List;
import java.util.UUID;

public interface WebSocketNotificationService {
    /**
     * Gửi thông báo trực tiếp đến một người dùng cụ thể qua WebSocket
     *
     * @param userId  ID của người nhận
     * @param message Dữ liệu thông báo
     */
    void sendToUser(UUID userId, NotificationSocketMessage message);

    /**
     * Gửi thông báo đến danh sách nhiều người dùng (tất cả user lấy ra từ Redis)
     *
     * @param userIds Danh sách ID người nhận
     * @param message Dữ liệu thông báo
     */
    void sendToUsers(List<UUID> userIds, NotificationSocketMessage message);

    /**
     * Broadcast thông báo tới một kênh topic chung
     *
     * @param destination Đường dẫn kênh (ví dụ: "/topic/sos-alerts")
     * @param payload     Dữ liệu cần phát
     */
    void broadcast(String destination, Object payload);
}
