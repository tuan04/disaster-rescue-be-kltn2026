package iuh.fit.notificationservice.kafka.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaTestRunner implements CommandLineRunner {

    private final KafkaTemplate<String, String> kafkaTemplate;

    // Gửi tin nhắn ngay khi ứng dụng Spring Boot vừa khởi chạy xong
    @Override
    public void run(String... args) throws Exception {
        System.out.println("🚀 Đang gửi tin nhắn test lên Kafka...");
        kafkaTemplate.send("test-topic", "Hello Kafka! Kết nối thành công!");
    }

    // Lắng nghe tin nhắn từ topic "test-topic"
    @KafkaListener(topics = "test-topic", groupId = "my-test-group")
    public void listen(String message) {
        System.out.println("✅ Đã nhận được tin nhắn từ Kafka: " + message);
    }
}