package iuh.fit.apigateway.filter;

import iuh.fit.apigateway.utils.JwtUtils;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private final JwtUtils jwtUtil;

    // Inject JwtUtil của bạn vào đây để giải mã token
    // @Autowired
    // private JwtUtil jwtUtil;
    public JwtAuthFilter(JwtUtils jwtUtil) {
        super(Config.class);       // Bắt buộc phải có để khởi tạo Filter
        this.jwtUtil = jwtUtil;    // Gán giá trị cho biến final
    }


    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || authHeader.trim().isEmpty()) {
                // Không có token -> Cho phép đi tiếp (cho các endpoint public/optional auth)
                return chain.filter(exchange);
            }

            if (authHeader.startsWith("Bearer ")) {
                authHeader = authHeader.substring(7);
            }

            try {
                UUID userId = jwtUtil.extractId(authHeader);

                if (userId != null) {
                    // Nhúng X-User-Id vào Header mới của Request chuyển tiếp
                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(r -> r.header("X-User-Id", userId.toString()))
                            .build();
                    return chain.filter(mutatedExchange);
                }

                return chain.filter(exchange);

            } catch (Exception e) {
                // Báo lỗi 401 nếu token hết hạn hoặc sai chữ ký
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete(); // Chặn request lại, không cho đi tiếp
    }

    public static class Config {
    }
}