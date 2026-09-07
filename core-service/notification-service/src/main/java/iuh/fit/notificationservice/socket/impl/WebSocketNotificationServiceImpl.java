package iuh.fit.notificationservice.socket.impl;

import iuh.fit.notificationservice.dtos.NotificationSocketMessage;
import iuh.fit.notificationservice.socket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationServiceImpl implements WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendToUser(UUID userId, NotificationSocketMessage message) {
        System.out.println("Sending notification to userId: " + userId + ", message: " + message);
        if (userId == null || message == null) {
            return;
        }

        String userIdStr = userId.toString();

//        // 1. Gửi qua User Destination chuẩn STOMP: /user/{userId}/queue/notifications
//        messagingTemplate.convertAndSendToUser(
//                userIdStr,
//                "/queue/notifications",
//                message
//        );

        // 2. Đồng thời gửi qua Topic đích danh: /topic/notifications/{userId}
        // Hỗ trợ trường hợp Client subscribe trực tiếp bằng ID mà không qua User Principal Session
        messagingTemplate.convertAndSend(
                "/topic/notifications/" + userIdStr,
                message
        );

    }

    @Override
    public void sendToUsers(List<UUID> userIds, NotificationSocketMessage message) {
        if (userIds == null || userIds.isEmpty() || message == null) {
            return;
        }



        for (UUID userId : userIds) {
            sendToUser(userId, message);
        }
    }

    @Override
    public void broadcast(String destination, Object payload) {
        if (destination == null || payload == null) {
            return;
        }
        messagingTemplate.convertAndSend(destination, payload);
    }
}
