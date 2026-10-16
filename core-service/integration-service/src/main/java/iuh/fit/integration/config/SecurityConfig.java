package iuh.fit.integration.config;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "CITIZEN",
            "RESCUER",
            "LEADER",
            "COORDINATOR",
            "ADMIN"
    );

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder,

            // Tái sử dụng khóa n8n đã cấu hình.
            @Value("${n8n.webhook.secret-key}")
            String newsWebhookKey,

            // Không cấu hình thì để rỗng.
            // Hàm kiểm tra bên dưới sẽ từ chối khóa rỗng.
            @Value("${sms.webhook.secret-key}")
            String smsWebhookKey
    ) throws Exception {

        return http

                // Một chain áp dụng cho toàn bộ HTTP request.
                // Không dùng securityMatcher giới hạn phạm vi.

                // Service xác thực bằng Bearer hoặc API key trong header.
                // Không dùng cookie/session làm thông tin đăng nhập.
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

                        // Cho phép servlet xử lý lỗi nội bộ.
                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR
                        ).permitAll()

                        // Người chưa đăng nhập vẫn được đọc tin tức.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/news",
                                "/api/v1/news/latest",
                                "/api/v1/news/{id}"
                        ).permitAll()

                        // n8n gửi tin tức bằng khóa hệ thống.
                        // Không yêu cầu n8n phải đăng nhập tài khoản ADMIN.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/news/ingest"
                        ).access((authentication, context) ->
                                new AuthorizationDecision(
                                        matchesApiKey(
                                                context.getRequest()
                                                        .getHeader("X-API-KEY"),
                                                newsWebhookKey
                                        )
                                )
                        )

                        // Hệ thống nhận SMS sử dụng khóa riêng.
                        // Khóa n8n không tự động có quyền gọi API này.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/sms"
                        ).access((authentication, context) ->
                                new AuthorizationDecision(
                                        matchesApiKey(
                                                context.getRequest()
                                                        .getHeader("X-API-KEY"),
                                                smsWebhookKey
                                        )
                                )
                        )

                        // Chỉ ADMIN được xem feed và chính sách tự duyệt.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/social-feeds",
                                "/api/v1/social-feeds/approval-policy"
                        ).hasRole("ADMIN")

                        // Chỉ ADMIN được duyệt từng feed.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/social-feeds/{feedId}/approve"
                        ).hasRole("ADMIN")

                        // Chỉ ADMIN được thay đổi chính sách.
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/social-feeds/approval-policy"
                        ).hasRole("ADMIN")


                        // Chat AI dành cho người đã đăng nhập.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/ai"
                        ).authenticated()

                        // Endpoint chưa khai báo sẽ bị từ chối.
                        .anyRequest().denyAll()
                )

                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(
                                        authenticationConverter()
                                )
                        )
                )

                .build();
    }

    private boolean matchesApiKey(
            String providedKey,
            String expectedKey
    ) {

        // Không chấp nhận thiếu khóa hoặc cấu hình khóa rỗng.
        // Tránh trường hợp cả hai cùng rỗng lại được xem là hợp lệ.
        if (providedKey == null || providedKey.isBlank()
                || expectedKey == null || expectedKey.isBlank()) {
            return false;
        }

        // So sánh byte bằng hàm dành cho so sánh giá trị bảo mật.
        // Không ghi giá trị khóa vào log.
        return MessageDigest.isEqual(
                expectedKey.getBytes(StandardCharsets.UTF_8),
                providedKey.getBytes(StandardCharsets.UTF_8)
        );
    }

    private Converter<Jwt, AbstractAuthenticationToken> authenticationConverter() {

        return jwt -> {

            // Chỉ đọc các claim sau khi decoder xác thực JWT thành công.
            Object rawId = jwt.getClaims().get("id");
            Object rawRole = jwt.getClaims().get("role");

            if (!(rawId instanceof String userId)) {
                throw invalidToken("Token thiếu hoặc sai định dạng id");
            }

            if (!(rawRole instanceof String role)
                    || !ALLOWED_ROLES.contains(role)) {
                throw invalidToken("Token có role không hợp lệ");
            }

            UUID parsedId;

            try {
                parsedId = UUID.fromString(userId);
            } catch (IllegalArgumentException exception) {
                throw invalidToken("id trong token không phải UUID");
            }

            if (!parsedId.toString().equalsIgnoreCase(userId)) {
                throw invalidToken("id trong token không phải UUID chuẩn");
            }

            // ADMIN trong JWT trở thành ROLE_ADMIN trong Spring Security.
            var authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );

            return new JwtAuthenticationToken(
                    jwt,
                    authorities,
                    parsedId.toString()
            );
        };
    }

    private OAuth2AuthenticationException invalidToken(String message) {
        return new OAuth2AuthenticationException(
                new OAuth2Error("invalid_token"),
                message
        );
    }
}