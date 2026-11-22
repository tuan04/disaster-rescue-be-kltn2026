package iuh.fit.notificationservice.redis;

import iuh.fit.notificationservice.client.UserRecipientGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class AllUserRecipientService {
    private static final String CACHE_KEY =
            "notification:all-user-ids";
    private static final Duration CACHE_TTL =
            Duration.ofMinutes(100);

    private final StringRedisTemplate redisTemplate;
    private final UserRecipientGrpcClient userRecipientGrpcClient;

    public List<UUID> findAllUserIds() {
        List<UUID> cachedUserIds = readCache();

        if (cachedUserIds != null) {
            return cachedUserIds;
        }

        List<UUID> userIds =
                userRecipientGrpcClient.findAllUserIds();

        writeCache(userIds);

        return userIds;
    }


    private List<UUID> readCache() {
        try {
            Set<String> members = redisTemplate.opsForSet()
                    .members(CACHE_KEY);

            if (members == null || members.isEmpty()) {
                return null;
            }

            return members.stream()
                    .map(UUID::fromString)
                    .toList();

        } catch (DataAccessException | IllegalArgumentException exception) {
            log.warn(
                    "Không đọc được cache người nhận, chuyển sang gRPC",
                    exception
            );

            return null;
        }
    }

    private void writeCache(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return;
        }
        // Ghi vào key tạm rồi thay thế key chính khi đã ghi xong.
        String temporaryKey = CACHE_KEY + ":tmp:" + UUID.randomUUID();

        String[] members = userIds.stream()
                .map(UUID::toString)
                .toArray(String[]::new);

        try {
            redisTemplate.opsForSet().add(temporaryKey, members);

            redisTemplate.expire(temporaryKey, CACHE_TTL);

            redisTemplate.rename(temporaryKey, CACHE_KEY);

        } catch (DataAccessException exception) {
            log.warn(
                    "Không ghi được cache người nhận",
                    exception
            );

        } finally {
            try {
                redisTemplate.delete(temporaryKey);
            } catch (DataAccessException exception) {
                log.debug("Không xóa được key tạm {}", temporaryKey, exception);
            }
        }
    }


}
