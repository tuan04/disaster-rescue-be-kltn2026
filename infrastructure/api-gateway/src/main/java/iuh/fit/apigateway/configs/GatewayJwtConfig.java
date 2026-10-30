package iuh.fit.apigateway.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import java.io.InputStream;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;


@Configuration
public class GatewayJwtConfig {
    private static final Set<String> ALLOWED_ROLES = Set.of(
            "CITIZEN",
            "RESCUER",
            "LEADER",
            "COORDINATOR",
            "ADMIN"
    );

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(
            @Value("${rsa.public-key-path}") Resource publicKeyResource
    ) throws Exception {

        RSAPublicKey publicKey;

        try (InputStream input = publicKeyResource.getInputStream()) {
            publicKey = RsaKeyConverters.x509().convert(input);
        }

        if (publicKey == null) {
            throw new IllegalStateException(
                    "Không đọc được RSA public key của gateway"
            );
        }

        // Decoder dành cho ứng dụng WebFlux.
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                .withPublicKey(publicKey)

                // Phải khớp thuật toán user-service dùng để ký JWT.
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(

                        // Kiểm tra issuer và thời gian hiệu lực.
                        JwtValidators.createDefaultWithIssuer(
                                "disaster-rescue"
                        ),

                        // Không nhận token thiếu thời điểm hết hạn.
                        new JwtClaimValidator<Instant>(
                                "exp",
                                expiration -> expiration != null
                        ),

                        // Token phải được cấp cho API backend.
                        new JwtClaimValidator<List<String>>(
                                "aud",
                                audiences -> audiences != null
                                        && audiences.contains("rescue-api")
                        ),

                        // Chỉ dùng access token trong Authorization: Bearer.
                        new JwtClaimValidator<String>(
                                "token_type",
                                "ACCESS"::equals
                        ),

                        // ID hợp lệ để tạo principal và phục vụ rate limit.
                        new JwtClaimValidator<Object>(
                                "id",
                                this::isValidUserId
                        ),

                        // Role hợp lệ để chuyển tiếp danh tính nhất quán.
                        new JwtClaimValidator<Object>(
                                "role",
                                role -> role instanceof String value
                                        && ALLOWED_ROLES.contains(value)
                        )
                )
        );

        return decoder;
    }

    private boolean isValidUserId(Object value) {

        if (!(value instanceof String userId)) {
            return false;
        }

        try {
            UUID parsedId = UUID.fromString(userId);

            // Không nhận UUID dạng rút gọn.
            return parsedId.toString().equalsIgnoreCase(userId);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
