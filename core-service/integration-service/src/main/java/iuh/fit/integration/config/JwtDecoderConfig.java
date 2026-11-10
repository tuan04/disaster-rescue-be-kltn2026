package iuh.fit.integration.config;

import iuh.fit.integration.utils.KeyReaderUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;

@Configuration
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${rsa.public-key-path}") Resource publicKeyResource
    ) throws Exception {

        RSAPublicKey publicKey;

        // Đọc public key PEM bằng converter có sẵn của Spring.
        // Không cần import KeyReaderUtils từ service khác.
        try (InputStream input = publicKeyResource.getInputStream()) {

            // Đọc nội dung file PEM bằng UTF-8.
            String publicKeyPem = new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            publicKey = (RSAPublicKey) KeyReaderUtils.getPublicKeyFromString(publicKeyPem);
        }


        // Kiểm tra chữ ký bằng public key.
        // Chỉ chấp nhận RS256, khớp với user-service.
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        // Kiểm tra issuer và thời gian hiệu lực của token.
        OAuth2TokenValidator<Jwt> issuerAndTime =
                JwtValidators.createDefaultWithIssuer(
                        "disaster-rescue"
                );

        // Bắt buộc token có thời điểm hết hạn.
        OAuth2TokenValidator<Jwt> expirationRequired =
                new JwtClaimValidator<Instant>(
                        "exp",
                        expiration -> expiration != null
                );

        // Token phải được phát hành cho API backend.
        OAuth2TokenValidator<Jwt> audience =
                new JwtClaimValidator<List<String>>(
                        "aud",
                        values -> values != null
                                && values.contains("rescue-api")
                );

        // Refresh/reset token không được dùng để gọi API nghiệp vụ.
        OAuth2TokenValidator<Jwt> tokenType =
                new JwtClaimValidator<String>(
                        "token_type",
                        "ACCESS"::equals
                );

        // Tất cả điều kiện phải thành công.
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        issuerAndTime,
                        expirationRequired,
                        audience,
                        tokenType
                )
        );

        return decoder;
    }
}