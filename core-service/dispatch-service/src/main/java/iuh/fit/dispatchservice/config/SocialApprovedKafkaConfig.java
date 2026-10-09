package iuh.fit.dispatchservice.config;

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
public class SocialApprovedKafkaConfig {


    @Bean("socialApprovedKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String>
    socialApprovedKafkaListenerContainerFactory(
            @Qualifier("kafkaConsumerFactory")
            ConsumerFactory<Object, Object> existingConsumerFactory
    ) {

        // Kế thừa bootstrap servers, security và cấu hình hiện có.
        // Sao chép để không sửa factory đang phục vụ các listener khác.
        Map<String, Object> properties = new HashMap<>(
                existingConsumerFactory.getConfigurationProperties()
        );

        // Key do integration gửi là externalId dạng chuỗi.
        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        // Value là chuỗi JSON đã được lưu trong outbox integration.
        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        // Tắt tự commit theo chu kỳ của Kafka client.
        // Listener container sẽ quyết định lúc commit.
        properties.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false
        );

        // Khi group chưa có offset hợp lệ, đọc từ đầu dữ liệu còn được giữ.
        // Nếu đã có offset thì tiếp tục từ offset của group.
        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        // Mỗi poll lấy tối đa một record để đơn giản hóa lượt xử lý.
        properties.put(
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
                1
        );

        // Tạo consumer factory chuyên đọc String.
        var consumerFactory =
                new DefaultKafkaConsumerFactory<String, String>(properties);

        // Tạo factory cho listener social.
        var factory =
                new ConcurrentKafkaListenerContainerFactory<String, String>();

        // Gắn consumer factory vừa tạo.
        factory.setConsumerFactory(consumerFactory);

        // Một lần gọi listener xử lý một record Kafka.
        factory.setBatchListener(false);

        // Bắt đầu với một consumer thread trong mỗi instance dispatch.
        factory.setConcurrency(1);

        // Commit offset sau khi listener xử lý thành công từng record.
        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.RECORD);

        // Chờ kết quả commit offset theo cách đồng bộ.
        // Việc này không làm PostgreSQL và Kafka thành một transaction chung.
        factory.getContainerProperties()
                .setSyncCommits(true);

        // Khi listener lỗi, nghỉ 5 giây rồi thử lại.
        // Chưa đặt giới hạn số lần thử lại ở giai đoạn này.
        var errorHandler = new DefaultErrorHandler(
                new FixedBackOff(
                        5_000L,
                        FixedBackOff.UNLIMITED_ATTEMPTS
                )
        );

        // Thay danh sách phân loại mặc định:
        // mọi exception do handler xử lý đều được xem là có thể retry.
        // Tránh trường hợp một lỗi bị coi là fatal và được bỏ qua ngay.
        errorHandler.setClassifications(Map.of(), true);

        // Không cho phép commit một record chỉ vì error handler đã xử lý lỗi.
        // Record thành công vẫn được commit theo AckMode.RECORD.
        errorHandler.setAckAfterHandle(false);

        // Chỉ áp dụng cơ chế retry này cho factory social.
        factory.setCommonErrorHandler(errorHandler);

        // Đăng ký factory hoàn chỉnh vào Spring context.
        return factory;
    }

}
