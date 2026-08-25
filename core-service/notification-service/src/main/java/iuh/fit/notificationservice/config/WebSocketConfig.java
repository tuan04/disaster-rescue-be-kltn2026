package iuh.fit.notificationservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Kích hoạt In-Memory Broker cho /topic (kênh broadcast) và /queue (kênh riêng)
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix cho các message gửi từ client lên controller (@MessageMapping)
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix cho các kênh gửi riêng đến từng user cụ thể
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint WebSocket tiêu chuẩn cho Mobile & Web STOMP Client
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        // Endpoint WebSocket hỗ trợ SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}