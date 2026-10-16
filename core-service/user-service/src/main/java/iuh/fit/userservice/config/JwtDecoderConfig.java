package iuh.fit.userservice.config;


import iuh.fit.userservice.utils.JwtUtils;
import iuh.fit.userservice.utils.KeyReaderUtils;
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

        try (InputStream inputStream = publicKeyResource.getInputStream()) {

            // Đọc nội dung file PEM bằng UTF-8.
            String publicKeyPem = new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            publicKey = (RSAPublicKey) KeyReaderUtils.getPublicKeyFromString(publicKeyPem);
        }

        // Tạo decoder xác minh chữ ký bằng public key vừa đọc.
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        // Bộ kiểm tra mặc định của Spring:
        // - Kiểm tra thời hạn exp.
        // - Kiểm tra thời điểm được phép sử dụng nbf nếu có.
        // - Kiểm tra issuer bằng giá trị TOKEN_ISSUER.
        OAuth2TokenValidator<Jwt> issuerAndTimeValidator =
                JwtValidators.createDefaultWithIssuer(
                        JwtUtils.TOKEN_ISSUER
                );

        // Bắt buộc token phải có exp.
        OAuth2TokenValidator<Jwt> expirationRequiredValidator =
                new JwtClaimValidator<Instant>(
                        "exp",
                        expiration -> expiration != null
                );

        // aud phải chứa rescue-api.
        // Spring chuyển claim aud thành danh sách audience.
        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtClaimValidator<List<String>>(
                        "aud",
                        audiences -> audiences != null
                                && audiences.contains(
                                JwtUtils.API_AUDIENCE
                        )
                );

        // Chỉ chấp nhận token_type = ACCESS.
        // Refresh token và reset token không được dùng để gọi API nghiệp vụ.
        OAuth2TokenValidator<Jwt> accessTokenValidator =
                new JwtClaimValidator<String>(
                        JwtUtils.TOKEN_TYPE_CLAIM,
                        JwtUtils.ACCESS_TOKEN_TYPE::equals
                );

        // Tất cả các bộ kiểm tra đều phải thành công.
        // Chỉ một điều kiện sai thì token bị từ chối.
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        issuerAndTimeValidator,
                        expirationRequiredValidator,
                        audienceValidator,
                        accessTokenValidator
                )
        );

        return decoder;
    }
}
