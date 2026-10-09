package iuh.fit.dispatchservice.kafka.producer;


import iuh.fit.common.kafka.dto.SOSResponse;
import iuh.fit.common.social.SocialEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class SocialSosNotificationProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(SOSResponse event) {
        try {
            // Truyền object event trực tiếp vào KafkaTemplate.
            // Không gọi ObjectMapper và không lưu notification outbox.
            kafkaTemplate.send(
                    SocialEvents.NOTIFICATION_TOPIC,
                    event.id().toString(),
                    event
            );

        } catch (RuntimeException exception) {

            // Xử lý lỗi xảy ra ngay khi gọi send().
            // Lỗi thông báo không làm lỗi luồng tạo yêu cầu cứu hộ.
            log.error(
                    "Loi khi gui su kien thong bao",
                    exception
            );
        }
    }
}
