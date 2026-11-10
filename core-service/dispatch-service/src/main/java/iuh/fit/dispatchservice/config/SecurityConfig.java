package iuh.fit.dispatchservice.config;


import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
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
import org.springframework.core.convert.converter.Converter;

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
            JwtDecoder jwtDecoder
    ) throws Exception {
        return http

                // Không giới hạn bằng securityMatcher:
                // chain này bao phủ toàn bộ HTTP request của service.

                // Dispatch chỉ xác thực bằng Bearer header,
                // không dùng cookie hoặc session để đăng nhập.
                // Vì vậy có thể tắt CSRF tại service này.
                .csrf(AbstractHttpConfigurer::disable)

                // Không lưu Authentication vào HTTP session.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // Không lưu request để chuyển hướng sau đăng nhập.
                .requestCache(AbstractHttpConfigurer::disable)

                // Không sử dụng các cơ chế đăng nhập mặc định.
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        // Cho phép servlet xử lý lỗi nội bộ.
                        // Đây là ERROR dispatch, không phải mở công khai
                        // một request HTTP trực tiếp tới đường dẫn /error.
                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR
                        ).permitAll()

                        // Khách chưa đăng nhập vẫn được gửi yêu cầu cứu hộ.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/sos-requests"
                        ).permitAll()

                        // Danh sách khu vực hoạt động phục vụ phía người dùng.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/locations"
                        ).permitAll()

                        // DTO cập nhật SOS hiện có dữ liệu nghiệp vụ.
                        // Chỉ quản trị và điều phối được sửa qua API này.
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/sos-requests"
                        ).hasAnyRole("ADMIN", "COORDINATOR")

                        // Dữ liệu quản lý:
                        // đặt rule cụ thể trước rule /map-points/{id}.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/map-points/sos-requests",
                                "/api/v1/locations/pages"
                        ).hasAnyRole("ADMIN", "COORDINATOR")

                        // Tạo kho, điểm an toàn và khu vực.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/map-points/warehouses",
                                "/api/v1/map-points/safe-points",
                                "/api/v1/locations"
                        ).hasAnyRole("ADMIN", "COORDINATOR")

                        // Cập nhật dữ liệu quản lý.
                        // Hai phiên bản multipart và JSON của
                        // cập nhật hazard đều đi qua cùng rule này.
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/map-points/warehouses/{id}",
                                "/api/v1/map-points/safe-points/{id}",
                                "/api/v1/map-points/hazard-reports/{id}",
                                "/api/v1/locations/{id}"
                        ).hasAnyRole("ADMIN", "COORDINATOR")

                        // Mọi tài khoản hợp lệ được gửi báo cáo hiểm họa.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/map-points/hazard-reports"
                        ).authenticated()

                        // Các API bản đồ phục vụ nghiệp vụ cứu hộ.
                        // Chưa mở cho CITIZEN vì hiện chưa lọc SOS theo chủ.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/map-points",
                                "/api/v1/map-points/strategic-points",
                                "/api/v1/map-points/rescue-requests-pending",
                                "/api/v1/map-points/{id}",
                                "/api/v1/routes"
                        ).hasAnyRole(
                                "ADMIN",
                                "COORDINATOR",
                                "LEADER",
                                "RESCUER"
                        )

                        // Quản trị hoặc điều phối phân công đội cứu hộ.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/assignments/rescue-requests/{requestId}/assign"
                        ).hasAnyRole("ADMIN", "COORDINATOR")

                        // Đội trưởng thực hiện các thao tác của đội.
                        //
                        // Đây mới là kiểm tra vai trò.
                        // Service còn phải kiểm tra người gọi có quyền
                        // trên teamId/assignmentId cụ thể hay không.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/assignments/rescue-requests/{requestId}/accept",
                                "/api/v1/assignments/teams/{teamId}/rescue-requests/{requestId}/accept",
                                "/api/v1/assignments/teams/{teamId}/rescue-requests/{requestId}/reject",
                                "/api/v1/assignments/{assignmentId}/complete",
                                "/api/v1/assignments/{assignmentId}/cancel"
                        ).hasRole("LEADER")

                        // Đọc thông tin phân công.
                        //
                        // Bước tiếp theo cần giới hạn LEADER/RESCUER
                        // vào đội hoặc nhiệm vụ mà họ được phép xem.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/assignments/teams/{teamId}/pending",
                                "/api/v1/assignments/teams/{teamId}/active",
                                "/api/v1/assignments/rescue-requests/{requestId}/active",
                                "/api/v1/assignments/{requestId}"
                        ).hasAnyRole(
                                "ADMIN",
                                "COORDINATOR",
                                "LEADER",
                                "RESCUER"
                        )

                        // Không tự động mở các endpoint được thêm về sau.
                        .anyRequest().denyAll()
                )

                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt

                                // Kiểm tra chữ ký và các claim của token.
                                .decoder(jwtDecoder)

                                // Chuyển token hợp lệ thành danh tính và quyền.
                                .jwtAuthenticationConverter(
                                        authenticationConverter()
                                )
                        )
                )

                .build();
    }



    private Converter<Jwt, AbstractAuthenticationToken>
    authenticationConverter() {

        return jwt -> {

            // Các claim này đã được bảo vệ bởi chữ ký JWT.
            Object rawId = jwt.getClaims().get("id");
            Object rawRole = jwt.getClaims().get("role");

            // Người gọi phải có ID dạng chuỗi.
            if (!(rawId instanceof String userId)) {
                throw invalidToken("Token thiếu hoặc sai định dạng id");
            }

            // Role phải thuộc danh sách hệ thống hỗ trợ.
            if (!(rawRole instanceof String role)
                    || !ALLOWED_ROLES.contains(role)) {
                throw invalidToken("Token có role không hợp lệ");
            }

            // Chuyển ID về dạng UUID chuẩn.
            UUID parsedId;

            try {
                parsedId = UUID.fromString(userId);
            } catch (IllegalArgumentException exception) {
                throw invalidToken("id trong token không phải UUID");
            }

            // Không chấp nhận UUID rút gọn.
            if (!parsedId.toString().equalsIgnoreCase(userId)) {
                throw invalidToken("id trong token không phải UUID chuẩn");
            }

            // Ví dụ LEADER -> ROLE_LEADER.
            var authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );

            // Authentication.getName() trả về UUID của người gọi.
            // Principal vẫn là Jwt để controller đọc bằng
            // @AuthenticationPrincipal Jwt jwt.
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
