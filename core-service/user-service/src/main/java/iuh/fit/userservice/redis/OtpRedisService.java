package iuh.fit.userservice.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpRedisService {
    
    private final StringRedisTemplate stringRedisTemplate;
    
    public String generateAndSaveOtp(String key) {
        SecureRandom random = new SecureRandom();
        int otpValue = random.nextInt(1000000);
        String otp = String.format("%06d", otpValue);
        
        stringRedisTemplate.opsForValue().set(key, otp, 5, TimeUnit.MINUTES);
        return otp;
    }
    public boolean verifyOtp(String key, String otp) {
        String savedOtp = stringRedisTemplate.opsForValue().get(key);
        return savedOtp != null && savedOtp.equals(otp);
    }
    
}
