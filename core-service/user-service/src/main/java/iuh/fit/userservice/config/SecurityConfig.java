package iuh.fit.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.convert.converter.Converter;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Configuration
public class SecurityConfig {
    private static final Set<String> ALLOWED_ROLES = Set.of(
            "CITIZEN",
            "RESCUER",
            "LEADER",
            "COORDINATOR",
            "ADMIN"
    );
    @Bean
    @Order(1)
    public SecurityFilterChain userApiSecurityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder
    ) throws Exception {
        return http
                .securityMatcher(
                        "/api/v1/users/**",
                        "/api/v1/auth/user-info",
                        "/api/v1/auth/logout"
                )
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
                        // Người có access token hợp lệ được xem hồ sơ của mình.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/users/me",
                                "/api/v1/users/profile",
                                "/api/v1/auth/user-info"
                        ).authenticated()

                        // Logout cần access token hợp lệ.
                        // Kiểm tra và thu hồi refresh token nằm ở controller/service.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/logout"
                        ).authenticated()

                        // Danh sách người dùng dành cho quản trị và điều phối.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/users"
                        ).hasAnyRole("ADMIN", "COORDINATOR")

                        // Danh sách ID và tên phục vụ nghiệp vụ quản lý đội.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/users/names"
                        ).hasAnyRole(
                                "ADMIN",
                                "COORDINATOR",
                                "LEADER"
                        )

                        // Chỉ ADMIN được duyệt yêu cầu trở thành người cứu hộ.
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/users/upgrade-rescuer-accept/{id}"
                        ).hasRole("ADMIN")

                        // Người dân chỉ được gửi yêu cầu nâng cấp cho chính mình.
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/users/upgrade-rescuer-request/{userId}"
                        ).access((authentication, context) -> {

                            // Authentication đã được tạo từ JWT hợp lệ.
                            Authentication currentUser = authentication.get();

                            // ID lấy từ biến đường dẫn {userId}.
                            String requestedUserId =
                                    context.getVariables().get("userId");

                            // Kiểm tra người gọi có vai trò CITIZEN.
                            boolean isCitizen = currentUser
                                    .getAuthorities()
                                    .stream()
                                    .anyMatch(authority ->
                                            "ROLE_CITIZEN".equals(
                                                    authority.getAuthority()
                                            )
                                    );

                            // getName() được converter bên dưới gán bằng user ID.
                            // Không lấy danh tính từ X-User-Id do client gửi.
                            boolean isOwner = currentUser
                                    .getName()
                                    .equals(requestedUserId);

                            // Phải đồng thời đúng vai trò và đúng chủ tài khoản.
                            return new AuthorizationDecision(
                                    isCitizen && isOwner
                            );
                        })

                        // Endpoint mới trong phạm vi chain này phải được
                        // khai báo quyền rõ ràng trước khi được sử dụng.
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt

                                // Kiểm tra chữ ký, thời gian, issuer,
                                // audience và token_type bằng decoder đã tạo.
                                .decoder(jwtDecoder)

                                // Sau khi token hợp lệ:
                                // chuyển id và role thành Authentication.
                                .jwtAuthenticationConverter(
                                        userAuthenticationConverter()
                                )
                        )
                )
                .build();
    }


    private Converter<Jwt, AbstractAuthenticationToken> userAuthenticationConverter() {

        return jwt -> {

            // Đọc dữ liệu từ JWT đã được xác minh chữ ký.
            Object rawUserId = jwt.getClaims().get("id");
            Object rawRole = jwt.getClaims().get("role");

            // ID bắt buộc là chuỗi.
            if (!(rawUserId instanceof String userId)) {
                throw invalidToken("Token thiếu hoặc sai định dạng id");
            }

            // Role bắt buộc là chuỗi và nằm trong danh sách cho phép.
            if (!(rawRole instanceof String role)
                    || !ALLOWED_ROLES.contains(role)) {
                throw invalidToken("Token thiếu hoặc có role không hợp lệ");
            }

            try {
                UUID parsedId = UUID.fromString(userId);

                // Không nhận các dạng UUID rút gọn mà Java có thể parse được.
                if (!parsedId.toString().equalsIgnoreCase(userId)) {
                    throw invalidToken("id trong token không phải UUID chuẩn");
                }
            } catch (IllegalArgumentException exception) {
                throw invalidToken("id trong token không phải UUID hợp lệ");
            }

            // Ví dụ: ADMIN → ROLE_ADMIN.
            // hasRole("ADMIN") sẽ kiểm tra authority ROLE_ADMIN.
            var authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );

            // Principal chứa JWT.
            // Authorities chứa quyền.
            // Authentication.getName() trả về userId.
            return new JwtAuthenticationToken(
                    jwt,
                    authorities,
                    userId
            );
        };
    }


    private OAuth2AuthenticationException invalidToken(String message) {
        // Dùng exception của Spring Security để lỗi xác thực
        // được xử lý tại security filter và trả HTTP 401.
        return new OAuth2AuthenticationException(
                new OAuth2Error("invalid_token"),
                message
        );
    }


}
