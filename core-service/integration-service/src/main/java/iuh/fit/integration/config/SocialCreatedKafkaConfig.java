package iuh.fit.integration.config;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class SocialCreatedKafkaConfig {
    // Listener nhận kết quả sẽ tham chiếu đúng tên bean này.
    @Bean("socialCreatedKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String>
    socialCreatedKafkaListenerContainerFactory(
            @Qualifier("kafkaConsumerFactory")
            ConsumerFactory<Object, Object> existingConsumerFactory
    ) {

        // Kế thừa địa chỉ broker và cấu hình security hiện có.
        Map<String, Object> properties = new HashMap<>(
                existingConsumerFactory.getConfigurationProperties()
        );

        // Key được gửi dưới dạng chuỗi externalId.
        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        // Value được đọc thành chuỗi JSON.
        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        // Không để Kafka client tự commit theo thời gian.
        properties.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );

        // Group chưa có offset sẽ đọc từ đầu dữ liệu còn được Kafka giữ.
        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        // Mỗi poll lấy tối đa một record.
        properties.put(
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                1
        );

        // Tạo consumer factory riêng cho dữ liệu String.
        var consumerFactory =
                new DefaultKafkaConsumerFactory<String, String>(properties);

        // Tạo listener container factory.
        var factory =
                new ConcurrentKafkaListenerContainerFactory<String, String>();

        // Gắn cấu hình consumer.
        factory.setConsumerFactory(consumerFactory);

        // Một lần gọi listener xử lý một record.
        factory.setBatchListener(false);

        // Một consumer thread trong mỗi instance integration.
        factory.setConcurrency(1);

        // Commit sau khi listener xử lý thành công một record.
        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.RECORD);

        // Chờ kết quả commit offset đồng bộ.
        factory.getContainerProperties()
                .setSyncCommits(true);

        // Nếu xử lý thất bại, nghỉ 5 giây rồi thử lại.
        var errorHandler = new DefaultErrorHandler(
                new FixedBackOff(
                        5_000L,
                        FixedBackOff.UNLIMITED_ATTEMPTS
                )
        );

        // Cho phép retry mọi exception thuộc phạm vi handler xử lý.
        // Không dùng danh sách lỗi fatal mặc định để bỏ qua record.
        errorHandler.setClassifications(Map.of(), true);

        // Không commit record chỉ vì handler đã xử lý exception.
        errorHandler.setAckAfterHandle(false);

        // Áp dụng handler riêng cho listener kết quả.
        factory.setCommonErrorHandler(errorHandler);

        // Trả factory để Spring đăng ký bean.
        return factory;
    }
}
