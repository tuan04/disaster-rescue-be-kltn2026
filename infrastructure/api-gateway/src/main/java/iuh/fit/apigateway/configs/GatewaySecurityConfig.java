package iuh.fit.apigateway.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
            String[] allowedOrigins
    ) {

        CorsConfiguration configuration = new CorsConfiguration();

        // Các frontend được phép gọi gateway từ trình duyệt.
        configuration.setAllowedOrigins(
                Arrays.asList(allowedOrigins)
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        // Hỗ trợ Authorization, X-API-KEY, X-XSRF-TOKEN...
        configuration.setAllowedHeaders(List.of("*"));

        // Cho phép frontend gửi refresh cookie tới user-service.
        configuration.setAllowCredentials(true);

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ReactiveJwtDecoder reactiveJwtDecoder,
            CorsConfigurationSource corsConfigurationSource
    ) {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        // Chuyển role trong JWT thành authority của Authentication.
        // Gateway không dùng authority này để giới hạn từng endpoint.
        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        // Principal.getName() trả về userId thay vì số điện thoại.
        converter.setPrincipalClaimName("id");

        converter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        // Adapter để sử dụng converter trong WebFlux.
        ReactiveJwtAuthenticationConverterAdapter reactiveConverter =
                new ReactiveJwtAuthenticationConverterAdapter(converter);

        return http

                // Xử lý CORS trước xác thực.
                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource
                ))

                // Gateway không dùng cookie để xác thực người dùng.
                // CSRF của luồng refresh dùng cookie do user-service xử lý.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)

                // Không lưu Authentication vào WebSession.
                .securityContextRepository(
                        NoOpServerSecurityContextRepository.getInstance()
                )

                // Không lưu request để chuyển hướng sau đăng nhập.
                .requestCache(cache -> cache.requestCache(
                        NoOpServerRequestCache.getInstance()
                ))

                // Không yêu cầu đăng nhập hoặc phân quyền theo endpoint
                // tại gateway.
                //
                // Không có Bearer token: request được chuyển tiếp.
                // Có Bearer token sai: Resource Server vẫn từ chối.
                .authorizeExchange(auth -> auth
                        .anyExchange().permitAll()
                )

                // Bật xác thực Bearer token nếu request có gửi.
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt
                                .jwtDecoder(reactiveJwtDecoder)
                                .jwtAuthenticationConverter(
                                        reactiveConverter
                                )
                        )
                )

                .build();
    }
}