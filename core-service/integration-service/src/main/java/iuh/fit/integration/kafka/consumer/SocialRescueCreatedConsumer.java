package iuh.fit.integration.kafka.consumer;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.social.SocialEvents;
import iuh.fit.integration.services.SocialDispatchResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;


@Component
@RequiredArgsConstructor
@Slf4j
public class SocialRescueCreatedConsumer {
    // Chuyển JSON thành DTO.
    private final ObjectMapper objectMapper;

    // Kiểm tra và cập nhật kết quả vào MongoDB.
    private final SocialDispatchResultService resultService;
    @KafkaListener(
            // Topic do outbox dispatch gửi tới.
            topics = SocialEvents.CREATED_TOPIC,

            // Group của integration để nhận toàn bộ kết quả liên quan.
            // Notification sau này phải sử dụng group khác.
            groupId = "integration-social-created-group",

            // Dùng factory nhận String và retry vừa khai báo.
            containerFactory = "socialCreatedKafkaListenerContainerFactory"
    )
    public void onCreated(ConsumerRecord<String, String> record) {

        // Đọc JSON từ value của Kafka record.
        String payload = record.value();

        // Message rỗng không phải kết quả hợp lệ.
        if (!StringUtils.hasText(payload)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Payload kết quả dispatch không được để trống"
            );
        }

        // Mỗi Kafka record chứa một object Created.
        SocialEvents.Created event = objectMapper
                .readerFor(SocialEvents.Created.class)

                // Không chấp nhận dữ liệu JSON dư phía sau object.
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)

                // JSON không hợp lệ sẽ ném exception cho container xử lý.
                .readValue(payload);

        // Chỉ hoàn thành khi MongoDB đã nhận kết quả,
        // hoặc kết quả giống hệt đã được lưu trước đó.
        resultService.acceptCreated(event);

        // Ghi nhận việc đã tiếp nhận kết quả.
        // Log này cũng có thể xuất hiện khi Kafka giao lại cùng sự kiện.
        log.info(
                "Da ghi nhan ket qua dispatch, externalId={}, "
                        + "rescueRequestId={}, partition={}, offset={}",
                event.externalId(),
                event.rescue().id(),
                record.partition(),
                record.offset()
        );

        // Method kết thúc bình thường.
        // Container được phép commit offset theo AckMode.RECORD.
    }
}
