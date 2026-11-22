package iuh.fit.integration.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import iuh.fit.integration.dtos.response.WeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherCache {
    private static final String KEY_PREFIX = "weather:v2:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public String buildGpsKey(double gridLat, double gridLon, int days) {
        return String.format(Locale.ROOT, "%sgps:%.2f,%.2f:%d", KEY_PREFIX, gridLat, gridLon, days);
    }

    public String buildIpKey(String clientIp, int days) {
        String subnet = toSubnetKey(clientIp);
        return KEY_PREFIX + "ip:" + sha256Hex(subnet) + ":" + days;
    }

    public WeatherResponse get(String key) {
        try {
            String raw = redisTemplate.opsForValue().get(key);
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return objectMapper.readValue(raw, WeatherResponse.class);
        } catch (Exception ex) {
            log.warn("Không đọc được cache thời tiết từ Redis, bỏ qua cache: {}", ex.getClass().getSimpleName());
            return null;
        }
    }

    public void put(String key, WeatherResponse response) {
        if (response == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL);
        } catch (Exception ex) {
            log.warn("Không ghi được cache thời tiết vào Redis: {}", ex.getClass().getSimpleName());
        }
    }

    String toSubnetKey(String clientIp) {
        try {
            InetAddress addr = InetAddress.getByName(clientIp.trim());
            byte[] bytes = addr.getAddress();
            if (addr instanceof Inet4Address && bytes.length == 4) {
                // Gom nhóm /24 (3 byte đầu) cho IPv4.
                return (bytes[0] & 0xFF) + "." + (bytes[1] & 0xFF) + "." + (bytes[2] & 0xFF) + ".0/24";
            }
            if (addr instanceof Inet6Address && bytes.length == 16) {
                // Gom nhóm /48 (6 byte đầu) cho IPv6.
                return String.format(Locale.ROOT, "%02x%02x:%02x%02x:%02x%02x::/48",
                        bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5]);
            }
        } catch (Exception ignored) {
            // Fallback dùng nguyên chuỗi nếu không parse được.
        }
        return clientIp.trim();
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
