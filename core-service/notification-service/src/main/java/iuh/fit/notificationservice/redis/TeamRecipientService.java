package iuh.fit.notificationservice.redis;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.notificationservice.client.ResourceTeamGrpcClient;
import iuh.fit.notificationservice.dtos.TeamRecipientsSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TeamRecipientService {

    private static final String KEY_PREFIX =
            "notification:team-recipients:v1:";

    private final StringRedisTemplate redisTemplate;
    private final ResourceTeamGrpcClient resourceTeamGrpcClient;

    private static final long CACHE_TTL_SECONDS = 60*60*24; // 1 ngày

    public List<UUID> findUserIdsByTeamIds(List<UUID> teamIds) {
        if (teamIds == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Danh sách teamIds không được null"
            );
        }

        if (teamIds.isEmpty()) {
            return List.of();
        }

        // Lưu kết quả tra cứu tương ứng với từng đội.
        Map<UUID, List<UUID>> recipientsByTeam = new LinkedHashMap<>();

        // Những đội cần lấy dữ liệu từ resource-service.
        List<UUID> missingTeamIds = new ArrayList<>();

        for (UUID teamId : teamIds) {
            List<UUID> cachedUserIds = readUserIds(teamId);
            if (cachedUserIds == null) {
                missingTeamIds.add(teamId);
            } else {
                recipientsByTeam.put(teamId, cachedUserIds);
            }
        }

        if (!missingTeamIds.isEmpty()) {
            Map<UUID, TeamRecipientsSnapshot> fetched =
                    resourceTeamGrpcClient.findRecipientsByTeamIds(
                            missingTeamIds
                    );

            for (UUID teamId : missingTeamIds) {
                TeamRecipientsSnapshot snapshot = fetched.get(teamId);

                List<UUID> userIds = snapshot.teamExists()
                        ? snapshot.userIds()
                        : List.of();

                recipientsByTeam.put(teamId, userIds);

                writeUserIds(teamId, userIds);
            }
        }

        List<UUID> recipientIds = new ArrayList<>();

        for (UUID teamId : teamIds) {
            recipientIds.addAll(recipientsByTeam.get(teamId));
        }

        return List.copyOf(recipientIds);
    }

    private List<UUID> readUserIds(UUID teamId) {

        Set<String> members = redisTemplate.opsForSet()
                .members(cacheKey(teamId));

        if (members == null || members.isEmpty()) {
            return null;
        }

        // Redis lưu chuỗi; chuyển về UUID để code nghiệp vụ sử dụng.
        List<UUID> userIds = new ArrayList<>(members.size());

        for (String member : members) {
            userIds.add(UUID.fromString(member));
        }

        return userIds;

    }

    private void writeUserIds(UUID teamId, List<UUID> userIds) {
        String key = cacheKey(teamId);
        redisTemplate.delete(key);

        if (userIds.isEmpty()) {
            return;
        }

        String[] members = userIds.stream()
                .map(UUID::toString)
                .toArray(String[]::new);

        redisTemplate.opsForSet().add(key, members);

        redisTemplate.expire(
                key,
                Duration.ofSeconds(CACHE_TTL_SECONDS)
        );
    }

    private String cacheKey(UUID teamId) {
        return KEY_PREFIX + teamId;
    }



}
