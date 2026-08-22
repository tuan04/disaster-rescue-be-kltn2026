package iuh.fit.apigateway.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
    private static final long ACCESS_TOKEN_EXPIRATION = 60000*15; // 15 p
    private static final long RESET_PASSWORD_TOKEN_EXPIRATION = 300000; // 5 p
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000; // 7 ngày


    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtUtils(
            @Value("${rsa.private-key-path}") Resource privateKeyResource,
            @Value("${rsa.public-key-path}") Resource publicKeyResource
    ) throws Exception {
        String pirvateKeyStr = new String(privateKeyResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String publicKeyStr = new String(publicKeyResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        this.privateKey = KeyReaderUtils.getPrivateKeyFromString(pirvateKeyStr);
        this.publicKey = KeyReaderUtils.getPublicKeyFromString(publicKeyStr);
    }




    private String buildToken(Map<String,Object> claims, String subject, long expiration){
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public String generateResetToken(String phone, UUID userId){
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", userId);
        return buildToken(claims, phone, RESET_PASSWORD_TOKEN_EXPIRATION);
    }





    private Claims extractAllClaims(String token){
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
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return true;
        }
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


    public Date extractExpiration(String token){
        return extractClaim(token, Claims::getExpiration);
    }

}
