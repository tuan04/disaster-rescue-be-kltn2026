package iuh.fit.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Tắt CSRF (Bắt buộc với REST API dùng JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Cấu hình phân quyền các endpoint
                .authorizeHttpRequests(auth -> auth
                        // Mở cửa tự do cho toàn bộ các API bắt đầu bằng /api/v1/auth/
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // Các API khác (nếu có) bắt buộc phải có Token hợp lệ
                        .anyRequest().authenticated()
                )

                // 3. Cấu hình Session thành Stateless (Không lưu trạng thái phiên, vì ta dùng JWT)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }
}