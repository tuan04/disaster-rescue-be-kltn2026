package iuh.fit.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;

@Component
public class TrustedIdentityHeadersFilter implements GlobalFilter, Ordered {
    private static final String HEADER_CLIENT_IP = "X-Client-Ip";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String verifiedPublicIp = resolveVerifiedPublicClientIp(exchange.getRequest());

        // Xóa các header danh tính và IP do client tự khai để tránh giả mạo.
        ServerWebExchange cleanedExchange = exchange.mutate()
                .request(request -> request.headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Role");
                    headers.remove("X-Team-Id");
                    headers.remove(HEADER_CLIENT_IP);
                    if (verifiedPublicIp != null) {
                        headers.set(HEADER_CLIENT_IP, verifiedPublicIp);
                    }
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

    private String resolveVerifiedPublicClientIp(ServerHttpRequest request) {
        InetSocketAddress remoteSocket = request.getRemoteAddress();
        if (remoteSocket == null || remoteSocket.getAddress() == null) {
            return null;
        }

        InetAddress remoteAddress = remoteSocket.getAddress();
        String candidate = remoteAddress.getHostAddress();

        // Chỉ đọc header chuyển tiếp khi kết nối trực tiếp đến từ proxy/nội bộ tin cậy.
        if (isTrustedProxy(remoteAddress)) {
            String forwarded = extractForwardedIp(request);
            if (forwarded != null) {
                candidate = forwarded;
            }
        }

        return normalizePublicIp(candidate);
    }

    private boolean isTrustedProxy(InetAddress address) {
        return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress();
    }

    private String extractForwardedIp(ServerHttpRequest request) {
        // Ưu tiên X-Real-IP do Nginx ghi đè cứng từ $remote_addr, tránh client giả mạo đầu X-Forwarded-For.
        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] parts = forwardedFor.split(",");
            String rightmost = parts[parts.length - 1].trim();
            if (!rightmost.isBlank()) {
                return rightmost;
            }
        }
        return null;
    }

    private String normalizePublicIp(String rawIp) {
        if (rawIp == null || rawIp.isBlank()) {
            return null;
        }
        String cleaned = rawIp.trim();
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        int zoneIdx = cleaned.indexOf('%');
        if (zoneIdx > 0) {
            cleaned = cleaned.substring(0, zoneIdx);
        }
        // Validate định dạng IP literal, không cho phép hostname DNS lookup.
        if (!isIpLiteral(cleaned)) {
            return null;
        }
        try {
            InetAddress addr = InetAddress.getByName(cleaned);
            if (!isPublicAddress(addr)) {
                return null;
            }
            return addr.getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isIpLiteral(String value) {
        if (value.contains(":")) {
            return value.matches("^[0-9a-fA-F:.]+$");
        }
        return value.matches("^\\d{1,3}(\\.\\d{1,3}){3}$");
    }

    private boolean isPublicAddress(InetAddress addr) {
        if (addr.isAnyLocalAddress()
                || addr.isLoopbackAddress()
                || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress()
                || addr.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = addr.getAddress();
        if (addr instanceof Inet4Address && bytes.length == 4) {
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;
            // Loại bỏ CGNAT 100.64.0.0/10 và TEST-NET / benchmark / 0.0.0.0/8.
            if (first == 0) return false;
            if (first == 100 && (second >= 64 && second <= 127)) return false;
            if (first >= 224) return false;
        } else if (addr instanceof Inet6Address && bytes.length == 16) {
            int first = bytes[0] & 0xFF;
            // Loại bỏ Unique Local Address fc00::/7.
            if ((first & 0xFE) == 0xFC) return false;
        }
        return true;
    }
}
