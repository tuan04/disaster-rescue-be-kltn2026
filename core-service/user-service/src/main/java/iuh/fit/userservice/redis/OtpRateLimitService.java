package iuh.fit.userservice.redis;


import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OtpRateLimitService {
    private final StringRedisTemplate redisTemplate;

    public void acquireForgotPasswordPermit(String phone) {
        String key = "auth:otp:forgot-password:cooldown:" + phone;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(60));

        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException(
                    ErrorCode.TOO_MANY_REQUESTS,
                    "Please wait 60 seconds between OTP requests."
            );
        }
    }
}
