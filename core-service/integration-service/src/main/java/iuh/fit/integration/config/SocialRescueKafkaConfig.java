package iuh.fit.integration.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;


@Configuration(proxyBeanMethods = false)
public class SocialRescueKafkaConfig {

    @Bean("socialRescueKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String>
    socialRescueKafkaListenerContainerFactory(
            @Qualifier("kafkaConsumerFactory")
            ConsumerFactory<Object, Object> existingConsumerFactory
    ) {

        // Sao chép cấu hình Kafka hiện có do Spring Boot tạo từ YAML:
        // bootstrap servers, security và các consumer properties khác.
        // Không thay đổi factory đang dùng cho luồng SOS.
        Map<String, Object> properties = new HashMap<>(
                existingConsumerFactory.getConfigurationProperties()
        );

        // Topic mới nhận JSON dạng text từ crawler.
        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        // Để listener container quản lý commit sau khi xử lý thành công.
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Group mới đọc từ đầu các message còn được Kafka lưu giữ.
        // Nếu group đã có offset thì tiếp tục từ offset đã commit.
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Mỗi poll lấy tối đa một Kafka message.
        // Một message vẫn có thể chứa nhiều phần tử trong mảng JSON.
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);

        var consumerFactory =
                new DefaultKafkaConsumerFactory<String, String>(properties);

        var factory =
                new ConcurrentKafkaListenerContainerFactory<String, String>();

        factory.setConsumerFactory(consumerFactory);

        // Đây là record listener: một lần gọi xử lý một Kafka message.
        // Không bật batch listener chỉ vì JSON bên trong là một mảng.
        factory.setBatchListener(false);

        // Chỉ commit offset sau khi listener xử lý xong cả mảng.
        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.RECORD);

        // Bản cơ bản: nếu parse/validate/lưu thất bại thì dừng consumer
        // của luồng này để không âm thầm bỏ qua message lỗi.
        // Sau khi xử lý nguyên nhân, cần khởi động lại listener/service.
        factory.setCommonErrorHandler(
                new CommonContainerStoppingErrorHandler()
        );

        return factory;
    }
}
