package iuh.fit.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class TrustedIdentityHeadersFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Xóa các header danh tính mà client có thể tự giả mạo.
        ServerWebExchange cleanedExchange = exchange.mutate()
                .request(request -> request.headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Role");
                    headers.remove("X-Team-Id");
                }))
                .build();

        return exchange.getPrincipal()

                // Chỉ chấp nhận principal từ JWT đã xác thực.
                .ofType(JwtAuthenticationToken.class)

                .map(authentication -> {

                    String userId = UUID.fromString(
                            authentication.getToken()
                                    .getClaimAsString("id")
                    ).toString();

                    String role = authentication.getToken()
                            .getClaimAsString("role");

                    return cleanedExchange.mutate()
                            .request(request -> request.headers(headers -> {

                                // set bảo đảm chỉ có một giá trị header.
                                headers.set("X-User-Id", userId);
                                headers.set("X-User-Role", role);
                            }))
                            .build();
                })

                // Không đăng nhập: vẫn chuyển tiếp,
                // nhưng không giữ header danh tính giả của client.
                .defaultIfEmpty(cleanedExchange)

                // Chuyển tiếp đúng một lần.
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
