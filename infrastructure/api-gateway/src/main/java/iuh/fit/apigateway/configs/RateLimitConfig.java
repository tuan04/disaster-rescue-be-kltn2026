package iuh.fit.apigateway.configs;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimitConfig {


    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            InetSocketAddress remoteAddress =
                    exchange.getRequest().getRemoteAddress();

            if (remoteAddress == null
                    || remoteAddress.getAddress() == null) {
                return Mono.empty();
            }

            String ip = remoteAddress.getAddress().getHostAddress();

            return Mono.just("ip:" + ip);
        };
    }

    @Bean
    @Primary
    public KeyResolver clientKeyResolver(){
        return exchange -> exchange.getPrincipal()
                .ofType(JwtAuthenticationToken.class)
                .filter(authentication -> authentication.isAuthenticated())
                .map(authentication -> "user:" + authentication.getName())
                .switchIfEmpty(Mono.defer(() -> {
                    // Không có JWT: dùng địa chỉ IP kết nối tới gateway.
                    InetSocketAddress remoteAddress =
                            exchange.getRequest().getRemoteAddress();

                    if (remoteAddress == null
                            || remoteAddress.getAddress() == null) {
                        return Mono.<String>empty();
                    }

                    String ip = remoteAddress.getAddress().getHostAddress();

                    return Mono.just("ip:" + ip);
                }));
    }


}
