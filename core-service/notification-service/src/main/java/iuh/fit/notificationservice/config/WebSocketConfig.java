package iuh.fit.notificationservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Cấu hình TaskScheduler cho STOMP Heartbeat (duy trì kết nối sống qua Gateway)
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("ws-heartbeat-thread-");
        taskScheduler.initialize();

        // Kích hoạt In-Memory Broker cho /topic (kênh broadcast) và /queue (kênh riêng)
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(taskScheduler);

        // Prefix cho các message gửi từ client lên controller (@MessageMapping)
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix cho các kênh gửi riêng đến từng user cụ thể
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 1. Endpoint WebSocket tiêu chuẩn cho Mobile & Web STOMP Native Client
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        // 2. Endpoint WebSocket hỗ trợ SockJS fallback (dành cho Web nếu cần)
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}