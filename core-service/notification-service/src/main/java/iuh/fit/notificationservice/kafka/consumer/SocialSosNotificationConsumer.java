package iuh.fit.notificationservice.kafka.consumer;

import iuh.fit.common.kafka.dto.SOSResponse;
import iuh.fit.common.social.SocialEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import iuh.fit.notificationservice.services.NotificationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class SocialSosNotificationConsumer {
    private final NotificationService notificationService;
    @KafkaListener(
            topics = SocialEvents.NOTIFICATION_TOPIC,
            groupId = "notification-social-sos-group"
    )
    public void onNotification(SOSResponse event) {

        try {
            // Nhận thẳng SOSResponse, không parse String.
            // Xử lý giống thông báo cứu hộ thông thường.
            notificationService.processSOSEvent(event);

        } catch (RuntimeException exception) {

            // Theo yêu cầu của bạn: lỗi xử lý thì ghi log rồi kết thúc.
            // Không ném lại lỗi để container retry việc gửi thông báo.
            log.error(
                    "Xu ly thong bao social that bai",
                    exception
            );
        }
    }
}
