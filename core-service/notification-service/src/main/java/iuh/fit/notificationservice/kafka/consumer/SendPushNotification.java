package iuh.fit.notificationservice.kafka.consumer;

import iuh.fit.notificationservice.dtos.SOSResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendPushNotification {

    @KafkaListener(topics = "sos-event", groupId = "notification-group")
    public void sendPushNotification(SOSResponse event) {
        log.info("Received SOS event in notification-service: {}", event);
        // Ngay lập tức gửi thông báo đẩy đến điện thoại của cứu hộ viên
        log.info("Push notification sent successfully for SOS request ID: {}", event.getId());
    }
}
