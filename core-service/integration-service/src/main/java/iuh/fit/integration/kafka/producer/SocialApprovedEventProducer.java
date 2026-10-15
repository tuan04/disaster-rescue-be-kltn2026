package iuh.fit.integration.kafka.producer;

import iuh.fit.common.social.SocialEvents;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class SocialApprovedEventProducer {

    // Factory riêng cho luồng gửi JSON đã được lưu trong outbox.
    private final DefaultKafkaProducerFactory<String, String> producerFactory;

    // Template riêng sử dụng StringSerializer.
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Spring tự gọi constructor này khi khởi tạo bean.
    public SocialApprovedEventProducer(
            @Qualifier("kafkaProducerFactory")
            ProducerFactory<?, ?> existingProducerFactory
    ) {

        // Kế thừa bootstrap servers, security và các cấu hình hiện có.
        // Sao chép để không sửa cấu hình producer đang phục vụ luồng khác.
        Map<String, Object> properties = new HashMap<>(
                existingProducerFactory.getConfigurationProperties()
        );

        // externalId được dùng làm Kafka key dạng chuỗi.
        properties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        // approvalPayload đã là JSON nên gửi nguyên chuỗi đó.
        properties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        // Chờ tất cả replica đang trong ISR xác nhận theo cấu hình broker.
        properties.put(ProducerConfig.ACKS_CONFIG, "all");

        // Bật cơ chế chống trùng cho retry nội bộ của Kafka producer.
        // Không thay thế chống trùng nghiệp vụ ở dispatch.
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Cho phép producer tự thử lại lỗi tạm thời.
        // Tổng thời gian vẫn bị giới hạn bởi delivery.timeout.ms.
        properties.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);

        // Giá trị tương thích với enable.idempotence = true.
        properties.put(
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,
                5
        );

        // Cho phép gom các message trong một khoảng thời gian rất ngắn.
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 5);

        // Thời gian chờ phản hồi cho một request tới broker.
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10_000);

        // Giới hạn tổng thời gian producer cố giao một message.
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30_000);

        // Giới hạn thời gian send() có thể chặn khi đợi metadata/buffer.
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5_000);

        // Luồng này dùng outbox MongoDB và gửi Kafka không transaction.
        // Không kế thừa transactional.id nếu cấu hình gốc có khai báo.
        properties.remove(ProducerConfig.TRANSACTIONAL_ID_CONFIG);

        // Tạo factory riêng; không đăng ký nó thành ProducerFactory bean mới.
        this.producerFactory = new DefaultKafkaProducerFactory<>(properties);

        // Template này chỉ phục vụ bộ gửi outbox.
        this.kafkaTemplate = new KafkaTemplate<>(producerFactory);
    }

    // Hàm chỉ hoàn thành bình thường khi Kafka xác nhận gửi thành công.
    // Các lỗi được chuyển cho worker quyết định lịch thử lại.
    public void sendAndWait(
            String externalId,
            String payload
    ) throws InterruptedException, ExecutionException, TimeoutException {

        // Không gửi message thiếu key nghiệp vụ.
        if (!StringUtils.hasText(externalId)) {
            throw new IllegalArgumentException("OUTBOX_EXTERNAL_ID_MISSING");
        }

        // Không gửi message rỗng nếu dữ liệu outbox bị thiếu.
        if (!StringUtils.hasText(payload)) {
            throw new IllegalArgumentException("OUTBOX_PAYLOAD_MISSING");
        }

        // Gửi nguyên payload đã lưu; không tạo eventId mới.
        kafkaTemplate.send(
                        SocialEvents.APPROVED_TOPIC,
                        externalId,
                        payload
                )

                // Chờ kết quả lâu hơn delivery.timeout.ms một chút.
                // Nếu hết thời gian chờ hoặc gửi lỗi thì hàm ném exception.
                .get(35, TimeUnit.SECONDS);
    }

    // Spring gọi khi ứng dụng đóng bình thường.
    @PreDestroy
    public void close() {

        try {
            // Giải phóng tài nguyên thuộc template.
            kafkaTemplate.destroy();
        } finally {
            // Factory do mình tự tạo nên mình chịu trách nhiệm đóng nó.
            producerFactory.destroy();
        }
    }
}