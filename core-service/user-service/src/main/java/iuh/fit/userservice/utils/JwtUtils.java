package iuh.fit.userservice.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import iuh.fit.userservice.dto.response.UserInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtUtils {
    private static final long ACCESS_TOKEN_EXPIRATION = 60000 * 15; // 15 p
    private static final long RESET_PASSWORD_TOKEN_EXPIRATION = 300000; // 5 p
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000; // 7 ngày


    // Định danh hệ thống phát hành token.
    // Khi xác thực, backend phải kiểm tra token do đúng hệ thống này cấp.
    public static final String TOKEN_ISSUER = "disaster-rescue";

    // Access token dành cho các API nghiệp vụ của hệ thống.
    public static final String API_AUDIENCE = "rescue-api";

    // Refresh token và reset token chỉ dành cho user-service.
    public static final String AUTH_AUDIENCE = "user-service";

    // Tên claim dùng để xác định mục đích sử dụng token.
    public static final String TOKEN_TYPE_CLAIM = "token_type";

    // Các giá trị hợp lệ của claim token_type.
    public static final String ACCESS_TOKEN_TYPE = "ACCESS";
    public static final String REFRESH_TOKEN_TYPE = "REFRESH";
    public static final String RESET_TOKEN_TYPE = "RESET_PASSWORD";



    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtUtils(
            @Value("${rsa.private-key-path}") Resource privateKeyResource,
            @Value("${rsa.public-key-path}") Resource publicKeyResource
    ) throws Exception {
        String privateKeyStr = new String(privateKeyResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String publicKeyStr = new String(publicKeyResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        this.privateKey = KeyReaderUtils.getPrivateKeyFromString(privateKeyStr);
        this.publicKey = KeyReaderUtils.getPublicKeyFromString(publicKeyStr);
    }


    private String buildToken(
            Map<String, Object> claims,
            String subject,
            long expirationMillis,
            String tokenType,
            String audience
    ) {
        // Lấy thời điểm hiện tại một lần để tính iat và exp thống nhất.
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setClaims(new HashMap<>(claims))

                // iss: hệ thống phát hành token.
                .setIssuer(TOKEN_ISSUER)

                // aud: bên được phép nhận và sử dụng token.
                .setAudience(audience)

                // sub: giữ số điện thoại như code hiện tại của anh.
                // User ID vẫn nằm trong claim "id".
                .setSubject(subject)

                // jti: mã riêng của mỗi token.
                // Hai lần cấp token cho cùng người dùng vẫn có jti khác nhau.
                .setId(UUID.randomUUID().toString())

                // Claim riêng để phân biệt ACCESS, REFRESH và RESET_PASSWORD.
                .claim(TOKEN_TYPE_CLAIM, tokenType)

                // iat: thời điểm cấp token.
                .setIssuedAt(new Date(now))

                // exp: thời điểm hết hạn.
                // expirationMillis được tính bằng MILLISECOND.
                .setExpiration(new Date(now + expirationMillis))

                // Ký token bằng private key RSA của user-service.
                // Service nhận token sẽ dùng public key để kiểm tra chữ ký.
                .signWith(privateKey, SignatureAlgorithm.RS256)

                // Chuyển thành chuỗi JWT để trả về client.
                .compact();
    }


    public String generateResetToken(String phone, UUID userId) {
        Map<String, Object> claims = new HashMap<>();

        // Xác định tài khoản được phép đặt lại mật khẩu.
        claims.put("id", userId.toString());

        // Không cần thêm role hoặc teamId:
        // reset token không được dùng để thực hiện nghiệp vụ.
        return buildToken(
                claims,
                phone,

                // Giữ thời hạn hiện tại: 5 phút.
                RESET_PASSWORD_TOKEN_EXPIRATION,

                // Chỉ dành cho thao tác đặt lại mật khẩu.
                RESET_TOKEN_TYPE,

                // Chỉ user-service được xử lý.
                AUTH_AUDIENCE
        );
    }

    private Map<String, Object> buildUserClaims(
            UserInfoResponse userInfo
    ) {
        // HashMap cho phép teamId là null khi người dùng chưa thuộc đội nào.
        Map<String, Object> claims = new HashMap<>();

        // Lưu UUID dưới dạng String để các service đọc thống nhất.
        claims.put("id", userInfo.getId().toString());

        // Vai trò hiệu lực do backend xác định.
        // Ví dụ: CITIZEN, RESCUER, LEADER, COORDINATOR, ADMIN.
        claims.put("role", userInfo.getRole());

        // Giữ thông tin đang được frontend sử dụng.
        claims.put("fullName", userInfo.getFullName());

        // Người dân hoặc người chưa được phân đội có thể không có teamId.
        claims.put(
                "teamId",
                userInfo.getTeamId() == null
                        ? null
                        : userInfo.getTeamId().toString()
        );

        return claims;
    }


    public String generateAccessToken(UserInfoResponse userInfo) {
        // Chuẩn bị thông tin người dùng cần đưa vào token.
        Map<String, Object> claims = buildUserClaims(userInfo);

        return buildToken(
                claims,
                userInfo.getPhone(),

                // Giữ thời hạn hiện tại: 15 phút.
                ACCESS_TOKEN_EXPIRATION,

                // Token này được dùng để gọi API nghiệp vụ.
                ACCESS_TOKEN_TYPE,

                // Các API backend là đối tượng nhận token.
                API_AUDIENCE
        );
    }


    public String generateRefreshToken(UserInfoResponse userInfo) {
        // Giữ các claim hiện có để không làm thiếu dữ liệu
        // mà controller refresh hiện tại đang đọc.
        Map<String, Object> claims = buildUserClaims(userInfo);

        return buildToken(
                claims,
                userInfo.getPhone(),

                // Giữ thời hạn hiện tại: 7 ngày.
                REFRESH_TOKEN_EXPIRATION,

                // Token chỉ có mục đích xin access token mới.
                REFRESH_TOKEN_TYPE,

                // Chỉ user-service xử lý loại token này.
                AUTH_AUDIENCE
        );
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractClaim(token, Claims::getExpiration).before(new Date());
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    public UUID extractTeamId(String token) {
        final Claims claims = extractAllClaims(token);
        Object teamIdObj = claims.get("teamId");
        if (teamIdObj == null) return null;
        return teamIdObj instanceof String ? UUID.fromString((String) teamIdObj) : (UUID) teamIdObj;
    }

    public String extractRole(String token) {
        final Claims claims = extractAllClaims(token);
        Object roleObj = claims.get("role");
        if (roleObj == null) return null;
        return roleObj.toString();
    }

    public Long extractTimeRemaining(String token) {
        Date expirationDate = extractExpiration(token);
        Long currentTimeMillis = System.currentTimeMillis();
        return expirationDate.getTime() - currentTimeMillis;
    }

    public String extractPhone(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID extractId(String token) {
        final Claims claims = extractAllClaims(token);
        Object id = claims.get("id");
        if (id == null) return null;
        return id instanceof String ? UUID.fromString((String) id) : (UUID) id;
    }

    public String extractFullName(String token) {
        final Claims claims = extractAllClaims(token);
        return claims.get("fullName", String.class);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
