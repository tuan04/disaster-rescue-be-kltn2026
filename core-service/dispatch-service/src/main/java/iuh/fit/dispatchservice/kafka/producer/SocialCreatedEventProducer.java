package iuh.fit.dispatchservice.kafka.producer;


import org.apache.kafka.common.serialization.StringSerializer;
import iuh.fit.common.social.SocialEvents;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.producer.ProducerConfig;
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
public class SocialCreatedEventProducer {
    // Factory riêng của luồng gửi kết quả social.
    private final DefaultKafkaProducerFactory<String, String> producerFactory;

    // Template sử dụng StringSerializer cho cả key và value.
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Spring inject factory gốc để lấy cấu hình kết nối Kafka.
    public SocialCreatedEventProducer(
            @Qualifier("kafkaProducerFactory")
            ProducerFactory<?, ?> existingProducerFactory
    ) {

        // Kế thừa bootstrap servers và security.
        // Sao chép để không sửa producer đang dùng ở các luồng khác.
        Map<String, Object> properties = new HashMap<>(
                existingProducerFactory.getConfigurationProperties()
        );

        // Kafka key là externalId dạng chuỗi.
        properties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        // Gửi nguyên JSON đã lưu, không serialize chuỗi thêm lần nữa.
        properties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        // Chờ xác nhận theo chế độ acks=all của Kafka.
        properties.put(ProducerConfig.ACKS_CONFIG, "all");

        // Chống trùng do retry nội bộ của producer.
        // Không loại bỏ được mọi lần gửi lại từ ứng dụng.
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Cho phép producer retry lỗi tạm thời trong thời gian giao cho phép.
        properties.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);

        // Giới hạn tương thích với producer idempotent.
        properties.put(
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,
                5
        );

        // Khoảng chờ ngắn để producer có thể gom message.
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 5);

        // Giới hạn thời gian chờ một request tới broker.
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10_000);

        // Giới hạn tổng thời gian producer cố giao message.
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30_000);

        // Giới hạn thời gian send() chờ metadata hoặc buffer.
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5_000);

        // Luồng outbox này gửi Kafka không transaction.
        properties.remove(ProducerConfig.TRANSACTIONAL_ID_CONFIG);

        // Tạo factory riêng dưới sự quản lý của class này.
        this.producerFactory =
                new DefaultKafkaProducerFactory<>(properties);

        // Không đăng ký thêm KafkaTemplate bean để tránh ảnh hưởng
        // template tự cấu hình mà các producer khác đang sử dụng.
        this.kafkaTemplate = new KafkaTemplate<>(producerFactory);
    }

    // Chỉ hoàn thành bình thường khi Kafka xác nhận gửi thành công.
    public void sendAndWait(
            String topic,
            String messageKey,
            String payload
    ) throws InterruptedException, ExecutionException, TimeoutException {

        // Bảng outbox này chỉ phục vụ sự kiện social-rescue-created.
        // Dữ liệu sai topic phải báo lỗi để giữ lại và xử lý.
        if (!SocialEvents.CREATED_TOPIC.equals(topic)) {
            throw new IllegalArgumentException("OUTBOX_TOPIC_INVALID");
        }

        // Không gửi nếu thiếu externalId dùng làm key.
        if (!StringUtils.hasText(messageKey)) {
            throw new IllegalArgumentException("OUTBOX_KEY_MISSING");
        }

        // Không gửi nếu thiếu JSON sự kiện.
        if (!StringUtils.hasText(payload)) {
            throw new IllegalArgumentException("OUTBOX_PAYLOAD_MISSING");
        }

        // Gửi nguyên dữ liệu đã lưu trong outbox.
        kafkaTemplate.send(topic, messageKey, payload)

                // Đợi kết quả với thời gian giới hạn.
                // Lỗi hoặc timeout sẽ được chuyển cho publisher xử lý.
                .get(35, TimeUnit.SECONDS);
    }

    // Giải phóng tài nguyên khi ứng dụng đóng bình thường.
    @PreDestroy
    public void close() {

        try {
            // Đóng tài nguyên của template.
            kafkaTemplate.destroy();
        } finally {
            // Factory được tự tạo nên cần tự đóng.
            producerFactory.destroy();
        }
    }
}
