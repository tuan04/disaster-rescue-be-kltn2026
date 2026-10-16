package iuh.fit.notificationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.io.InputStream;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Configuration
public class JwtDecoderConfig {

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "CITIZEN",
            "RESCUER",
            "LEADER",
            "COORDINATOR",
            "ADMIN"
    );

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${rsa.public-key-path}") Resource publicKeyResource
    ) throws Exception {

        RSAPublicKey publicKey;

        // Đọc public key dạng PEM và tự đóng luồng đọc file.
        try (InputStream input = publicKeyResource.getInputStream()) {
            publicKey = RsaKeyConverters.x509().convert(input);
        }

        if (publicKey == null) {
            throw new IllegalStateException(
                    "Không đọc được RSA public key"
            );
        }

        // Xác minh chữ ký bằng public key, chỉ nhận thuật toán RS256.
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(

                        // Kiểm tra issuer và thời gian hiệu lực.
                        JwtValidators.createDefaultWithIssuer(
                                "disaster-rescue"
                        ),

                        // Bắt buộc token có thời điểm hết hạn.
                        new JwtClaimValidator<Instant>(
                                "exp",
                                expiration -> expiration != null
                        ),

                        // Token phải dành cho API backend.
                        new JwtClaimValidator<List<String>>(
                                "aud",
                                audiences -> audiences != null
                                        && audiences.contains("rescue-api")
                        ),

                        // Không nhận refresh/reset token thay access token.
                        new JwtClaimValidator<String>(
                                "token_type",
                                "ACCESS"::equals
                        ),

                        // Bắt buộc có ID người dùng dạng UUID.
                        // Nhờ vậy controller có thể chuyển id sang UUID.
                        new JwtClaimValidator<Object>(
                                "id",
                                this::isValidUserId
                        ),

                        // Kiểm tra role trước khi chuyển thành authority.
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

        // Không nhận null hoặc kiểu dữ liệu khác String.
        if (!(value instanceof String userId)) {
            return false;
        }

        try {
            UUID parsedId = UUID.fromString(userId);

            // Kiểm tra định dạng đầy đủ, không nhận UUID rút gọn.
            return parsedId.toString().equalsIgnoreCase(userId);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}