package iuh.fit.userservice.redis;


import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginAttemptRedisService {
    private static final int MAX_FAILURES = 5;
    private static final long WINDOW_SECONDS = 15 * 60;
    private final StringRedisTemplate redisTemplate;

    private static final DefaultRedisScript<Long> RECORD_FAILURE =
            new DefaultRedisScript<>("""
                    local count = redis.call('INCR', KEYS[1])

                    if count == 1 then
                        redis.call('EXPIRE', KEYS[1], ARGV[1])
                    end

                    return count
                    """, Long.class);


    private String key(String phone) {
        return "auth:login-fail:" + phone;
    }


    public void checkAllowed(String phone) {
        String value = redisTemplate.opsForValue().get(key(phone));

        if (value != null
                && Long.parseLong(value) >= MAX_FAILURES) {
            throw new BusinessException(
                    ErrorCode.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Please try again later."
            );
        }
    }

    public void recordFailure(String phone) {
        redisTemplate.execute(
                RECORD_FAILURE,
                List.of(key(phone)),
                String.valueOf(WINDOW_SECONDS)
        );
    }

    public void reset(String phone) {
        redisTemplate.delete(key(phone));
    }


}
