package iuh.fit.dispatchservice.config;

import iuh.fit.dispatchservice.utils.KeyReaderUtils;
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

    // Phải khớp với TOKEN_ISSUER trong JwtUtils của user-service.
    private static final String TOKEN_ISSUER = "disaster-rescue";

    // Access token được phát hành cho các API backend.
    // Phải khớp với API_AUDIENCE của user-service.
    private static final String API_AUDIENCE = "rescue-api";

    // Claim dùng để phân biệt mục đích sử dụng token.
    private static final String TOKEN_TYPE_CLAIM = "token_type";

    // Dispatch-service chỉ nhận access token để xác thực HTTP API.
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";

    @Bean
    public JwtDecoder jwtDecoder(

            // Đọc đường dẫn từ cấu hình dispatch-service.yml.
            @Value("${rsa.public-key-path}") Resource publicKeyResource
    ) throws Exception {

        // Public key RSA dùng để xác minh chữ ký.
        RSAPublicKey publicKey;

        // Tự động đóng luồng đọc file sau khi hoàn tất.
        try (InputStream inputStream = publicKeyResource.getInputStream()) {
            // Đọc nội dung file PEM bằng UTF-8.
            String publicKeyPem = new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            publicKey = (RSAPublicKey) KeyReaderUtils.getPublicKeyFromString(publicKeyPem);
        }

        // Không cho ứng dụng khởi động với public key không hợp lệ.
        if (publicKey == null) {
            throw new IllegalStateException(
                    "Không đọc được RSA public key của dispatch-service"
            );
        }

        // Bộ xác minh chữ ký JWT.
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(publicKey)

                // Chỉ nhận thuật toán RS256,
                // khớp với thuật toán user-service dùng để ký token.
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        // Kiểm tra:
        // - iss phải là disaster-rescue.
        // - exp còn hiệu lực nếu có.
        // - nbf đã đến thời điểm được phép sử dụng nếu có.
        OAuth2TokenValidator<Jwt> issuerAndTimeValidator =
                JwtValidators.createDefaultWithIssuer(
                        TOKEN_ISSUER
                );

        // Bắt buộc có thời điểm hết hạn.
        // Token không có exp sẽ bị từ chối.
        OAuth2TokenValidator<Jwt> expirationRequiredValidator =
                new JwtClaimValidator<Instant>(
                        "exp",
                        expiration -> expiration != null
                );

        // Spring biểu diễn aud dưới dạng danh sách.
        // Danh sách đó phải chứa rescue-api.
        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtClaimValidator<List<String>>(
                        "aud",
                        audiences -> audiences != null
                                && audiences.contains(API_AUDIENCE)
                );

        // Không cho dùng REFRESH hoặc RESET_PASSWORD để gọi API.
        OAuth2TokenValidator<Jwt> accessTokenValidator =
                new JwtClaimValidator<String>(
                        TOKEN_TYPE_CLAIM,
                        ACCESS_TOKEN_TYPE::equals
                );

        // Kết hợp các điều kiện.
        // Token chỉ hợp lệ khi tất cả validator đều thành công.
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        issuerAndTimeValidator,
                        expirationRequiredValidator,
                        audienceValidator,
                        accessTokenValidator
                )
        );

        // Đăng ký decoder để SecurityFilterChain dùng ở phần tiếp theo.
        return decoder;
    }
}