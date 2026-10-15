package iuh.fit.dispatchservice.kafka.consumer;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.social.SocialEvents;
import iuh.fit.dispatchservice.services.SocialRescueDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import tools.jackson.databind.DeserializationFeature;

@Component
@RequiredArgsConstructor
@Slf4j
public class SocialApprovedConsumer {

    private final ObjectMapper objectMapper;

    private final SocialRescueDispatchService dispatchService;

    @KafkaListener(
            topics = SocialEvents.APPROVED_TOPIC,
            groupId = "dispatch-social-rescue-group",
            containerFactory = "socialApprovedKafkaListenerContainerFactory"
    )
    public void onApproved(ConsumerRecord<String, String> record) {
        // Đọc nguyên chuỗi JSON từ Kafka.
        // Nhận ConsumerRecord giúp xử lý rõ cả trường hợp value = null.
        String payload = record.value();

        if (!StringUtils.hasText(payload)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Payload phê duyệt không được để trống"
            );
        }

        // Tạo reader chuyên đọc SocialEvents.Approved.
        // Reader riêng không thay đổi cấu hình ObjectMapper dùng chung.
        SocialEvents.Approved event = objectMapper
                .readerFor(SocialEvents.Approved.class)

                // Không chấp nhận hai JSON object nối tiếp trong một payload.
                // Mỗi Kafka record phải chứa đúng một giá trị JSON.
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)

                // Nếu JSON sai cú pháp hoặc sai kiểu thì exception thoát ra.
                .readValue(payload);

        // Service kiểm tra validation rồi thực hiện transaction PostgreSQL.
        // Khi lời gọi này trả về bình thường, transaction của service đã commit.
        boolean created = dispatchService.acceptApproved(event);

        // true: vừa tạo yêu cầu mới và lưu outbox thành công.
        if (created) {
            log.info(
                    "Da tao yeu cau tu feed={}, eventId={}, partition={}, offset={}",
                    event.externalId(),
                    event.eventId(),
                    record.partition(),
                    record.offset()
            );

        } else {

            // false: externalId đã được xử lý trước đó.
            // Đây là kết quả hợp lệ khi Kafka giao lại sự kiện.
            log.debug(
                    "Feed da duoc xu ly, externalId={}, partition={}, offset={}",
                    event.externalId(),
                    record.partition(),
                    record.offset()
            );
        }

        // Không cần gọi acknowledge() thủ công.
        // Method kết thúc bình thường, container sẽ commit theo AckMode.RECORD.
    }


}

