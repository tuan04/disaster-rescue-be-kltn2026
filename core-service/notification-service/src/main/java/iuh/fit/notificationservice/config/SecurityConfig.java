package iuh.fit.notificationservice.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        // Đọc vai trò từ claim "role".
        authoritiesConverter.setAuthoritiesClaimName("role");

        // Ví dụ ADMIN -> ROLE_ADMIN.
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        // Authentication.getName() trả về ID người dùng.
        // Không dùng sub vì sub hiện đang chứa số điện thoại.
        converter.setPrincipalClaimName("id");

        converter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {

        return http

                // REST API xác thực bằng Bearer header,
                // không dùng cookie/session làm thông tin đăng nhập.
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth

                        // Cho phép xử lý lỗi nội bộ của servlet.
                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR
                        ).permitAll()

                        // Xem danh sách thông báo của chính mình.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/notifications"
                        ).authenticated()

                        // Đánh dấu đã đọc.
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/notifications/read-all",
                                "/api/v1/notifications/{id}/read"
                        ).authenticated()

                        // Xóa mềm một hoặc tất cả thông báo của mình.
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/notifications",
                                "/api/v1/notifications/{id}"
                        ).authenticated()

                        // Chưa mở /ws ở bước REST này.
                        // Các endpoint khác cũng bị từ chối mặc định.
                        .anyRequest().denyAll()
                )

                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                )

                .build();
    }
}