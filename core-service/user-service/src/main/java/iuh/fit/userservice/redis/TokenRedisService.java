package iuh.fit.userservice.redis;

import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class TokenRedisService {
    private final StringRedisTemplate stringRedisTemplate;

    public void revokeTokens(String key, String token, long expirationTimeInSeconds) {
        stringRedisTemplate.opsForValue().set(key, token, expirationTimeInSeconds, java.util.concurrent.TimeUnit.SECONDS);
    }

    public boolean isTokenRevoked(String key, String token) {
        String revokedToken = stringRedisTemplate.opsForValue().get(key);
        return revokedToken != null && revokedToken.equals(token);
    }
}
