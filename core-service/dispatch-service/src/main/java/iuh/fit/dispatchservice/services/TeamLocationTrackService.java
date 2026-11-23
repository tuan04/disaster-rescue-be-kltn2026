package iuh.fit.dispatchservice.services;

import iuh.fit.common.kafka.dto.TeamLocationUpdatedEvent;
import iuh.fit.dispatchservice.entity.TeamLocationLog;
import iuh.fit.dispatchservice.repositories.TeamLocationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamLocationTrackService {

    private static final String ACTIVE_ASSIGNMENT_KEY_PREFIX = "team:active_assignment:";
    private static final String ACTIVE_TEAMS_SET_KEY = "team:track:active_teams";
    private static final String BUFFER_KEY_PREFIX = "team:track:buffer:";
    private static final String FLUSH_LOCK_KEY_PREFIX = "lock:track:flush:";
    private static final Duration TRACK_TTL = Duration.ofHours(12);
    private static final Duration FLUSH_LOCK_TTL = Duration.ofSeconds(15);
    private static final int FINISH_LOCK_MAX_RETRIES = 20;
    private static final long FINISH_LOCK_RETRY_DELAY_MS = 50L;
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final double MIN_DISTANCE_METERS = 20.0;
    private static final double MAX_SPEED_MPS = 45.0; // ~162 km/h
    private static final int BATCH_FLUSH_THRESHOLD = 20;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private static final DefaultRedisScript<Long> CLEANUP_TRACK_SCRIPT = new DefaultRedisScript<>(
            "redis.call('DEL', KEYS[1]) " +
                    "if redis.call('GET', KEYS[2]) == ARGV[1] then " +
                    "    redis.call('DEL', KEYS[2]) " +
                    "    redis.call('SREM', KEYS[3], ARGV[2]) " +
                    "end " +
                    "return 1",
            Long.class
    );

    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                    "    return redis.call('DEL', KEYS[1]) " +
                    "end " +
                    "return 0",
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;
    private final TeamLocationLogRepository teamLocationLogRepository;

    public void startTracking(UUID campaignTeamId, UUID assignmentId) {
        if (campaignTeamId == null || assignmentId == null) {
            return;
        }
        runAfterCommit(() -> {
            try {
                String activeKey = ACTIVE_ASSIGNMENT_KEY_PREFIX + campaignTeamId;
                stringRedisTemplate.opsForValue().set(activeKey, assignmentId.toString(), TRACK_TTL);
                stringRedisTemplate.opsForSet().add(ACTIVE_TEAMS_SET_KEY, campaignTeamId.toString());
                stringRedisTemplate.delete(bufferKey(campaignTeamId, assignmentId));
            } catch (Exception ex) {
                log.warn("Không thể bật tracking hành trình trong Redis cho team={}, assignment={}: {}",
                        campaignTeamId, assignmentId, ex.getMessage());
            }
        });
    }

    public void appendPointIfActive(TeamLocationUpdatedEvent event) {
        if (event == null || event.teamId() == null || !isValidCoordinate(event.latitude(), event.longitude())) {
            return;
        }

        UUID teamId = event.teamId();
        try {
            String activeAssignmentRaw = stringRedisTemplate.opsForValue().get(ACTIVE_ASSIGNMENT_KEY_PREFIX + teamId);
            if (activeAssignmentRaw == null || activeAssignmentRaw.isBlank()) {
                return;
            }

            UUID assignmentId = UUID.fromString(activeAssignmentRaw.trim());
            String bufKey = bufferKey(teamId, assignmentId);
            long epochMs = event.recordedAt() != null ? event.recordedAt().toEpochMilli() : System.currentTimeMillis();

            String lastRaw = stringRedisTemplate.opsForList().index(bufKey, -1);
            if (lastRaw != null) {
                TrackPoint lastPoint = parsePoint(lastRaw);
                if (lastPoint != null) {
                    if (epochMs < lastPoint.epochMs()) {
                        return;
                    }
                    double distance = haversineMeters(lastPoint.latitude(), lastPoint.longitude(),
                            event.latitude(), event.longitude());
                    if (distance < MIN_DISTANCE_METERS) {
                        return;
                    }
                    double elapsedSeconds = Math.max(1.0, (epochMs - lastPoint.epochMs()) / 1000.0);
                    if ((distance / elapsedSeconds) > MAX_SPEED_MPS) {
                        log.debug("Bỏ qua điểm GPS nhảy cóc bất thường cho team={}: distance={}m, dt={}s",
                                teamId, distance, elapsedSeconds);
                        return;
                    }
                }
            }

            String encoded = encodePoint(event.longitude(), event.latitude(), epochMs);
            Long size = stringRedisTemplate.opsForList().rightPush(bufKey, encoded);
            stringRedisTemplate.expire(bufKey, TRACK_TTL);
            stringRedisTemplate.expire(ACTIVE_ASSIGNMENT_KEY_PREFIX + teamId, TRACK_TTL);

            if (size != null && size >= BATCH_FLUSH_THRESHOLD) {
                flushIntermediate(teamId, assignmentId);
            }
        } catch (Exception ex) {
            log.warn("Lỗi ghi điểm hành trình vào Redis cho team={}: {}", teamId, ex.getMessage());
        }
    }

    public void flushIntermediate(UUID campaignTeamId, UUID assignmentId) {
        if (campaignTeamId == null || assignmentId == null) {
            return;
        }
        String lockKey = FLUSH_LOCK_KEY_PREFIX + assignmentId;
        String lockToken = tryAcquireLock(lockKey);
        if (lockToken == null) {
            return;
        }
        try {
            String bufKey = bufferKey(campaignTeamId, assignmentId);
            List<String> rawPoints = stringRedisTemplate.opsForList().range(bufKey, 0, -1);
            if (rawPoints == null || rawPoints.size() < 2) {
                return;
            }
            // Lưu DB thành công trước rồi mới cắt Redis để không mất điểm GPS nếu DB lỗi.
            // LTRIM (N - 1) -1 giữ lại đúng điểm nối cuối cùng và mọi điểm mới được RPUSH trong lúc đang lưu DB.
            saveLineStringSegment(campaignTeamId, assignmentId, rawPoints);
            stringRedisTemplate.opsForList().trim(bufKey, rawPoints.size() - 1L, -1L);
        } catch (Exception ex) {
            log.warn("Lỗi flush giữa ca hành trình cho team={}, assignment={}: {}",
                    campaignTeamId, assignmentId, ex.getMessage());
        } finally {
            releaseLock(lockKey, lockToken);
        }
    }

    /**
     * Đọc phần điểm còn lại trong Redis lưu thành đoạn LineString cuối trong cùng transaction DB,
     * tính tổng quãng đường của ca cứu hộ, và chỉ xóa các key Redis sau khi DB commit thành công.
     * Giữ Redis lock xuyên suốt transaction để chặn tuyệt đối race condition với flushIntermediate().
     */
    public double finishTracking(UUID campaignTeamId, UUID assignmentId) {
        if (campaignTeamId == null || assignmentId == null) {
            return 0.0;
        }

        String bufKey = bufferKey(campaignTeamId, assignmentId);
        String activeKey = ACTIVE_ASSIGNMENT_KEY_PREFIX + campaignTeamId;
        String lockKey = FLUSH_LOCK_KEY_PREFIX + assignmentId;
        String lockToken = acquireLockWithRetry(lockKey);

        Runnable cleanupAction = () -> {
            try {
                stringRedisTemplate.execute(
                        CLEANUP_TRACK_SCRIPT,
                        List.of(bufKey, activeKey, ACTIVE_TEAMS_SET_KEY),
                        assignmentId.toString(),
                        campaignTeamId.toString()
                );
            } catch (Exception ex) {
                log.warn("Không thể dọn key Redis tracking sau commit cho team={}, assignment={}: {}",
                        campaignTeamId, assignmentId, ex.getMessage());
            }
        };

        Runnable releaseLockAction = () -> {
            if (lockToken != null) {
                releaseLock(lockKey, lockToken);
            }
        };

        boolean txActive = TransactionSynchronizationManager.isSynchronizationActive();
        if (txActive) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cleanupAction.run();
                }

                @Override
                public void afterCompletion(int status) {
                    releaseLockAction.run();
                }
            });
        }

        try {
            if (lockToken != null) {
                List<String> remainingPoints = stringRedisTemplate.opsForList().range(bufKey, 0, -1);
                saveLineStringSegment(campaignTeamId, assignmentId, remainingPoints);
            } else {
                log.warn("Không thể giữ lock flush cuối cho team={}, assignment={}; bỏ qua đọc buffer để tránh ghi trùng.",
                        campaignTeamId, assignmentId);
            }
        } catch (Exception ex) {
            log.warn("Lỗi đọc/lưu đoạn hành trình cuối cho team={}, assignment={}: {}",
                    campaignTeamId, assignmentId, ex.getMessage());
        } finally {
            if (!txActive) {
                cleanupAction.run();
                releaseLockAction.run();
            }
        }

        Double totalMeters = teamLocationLogRepository.sumDistanceMetersByAssignmentId(assignmentId);
        return totalMeters != null ? Math.round(totalMeters * 10.0) / 10.0 : 0.0;
    }

    @Scheduled(fixedDelay = 300_000)
    public void flushActiveTeamBuffers() {
        try {
            Set<String> activeTeamIds = stringRedisTemplate.opsForSet().members(ACTIVE_TEAMS_SET_KEY);
            if (activeTeamIds == null || activeTeamIds.isEmpty()) {
                return;
            }
            for (String teamIdRaw : activeTeamIds) {
                try {
                    UUID teamId = UUID.fromString(teamIdRaw);
                    String activeAssignmentRaw = stringRedisTemplate.opsForValue()
                            .get(ACTIVE_ASSIGNMENT_KEY_PREFIX + teamId);
                    if (activeAssignmentRaw == null || activeAssignmentRaw.isBlank()) {
                        stringRedisTemplate.opsForSet().remove(ACTIVE_TEAMS_SET_KEY, teamIdRaw);
                        continue;
                    }
                    UUID assignmentId = UUID.fromString(activeAssignmentRaw.trim());
                    flushIntermediate(teamId, assignmentId);
                } catch (Exception ex) {
                    log.warn("Lỗi flush định kỳ cho team={}: {}", teamIdRaw, ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("Không thể chạy flush định kỳ hành trình từ Redis: {}", ex.getMessage());
        }
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private void saveLineStringSegment(UUID campaignTeamId, UUID assignmentId, List<String> rawPoints) {
        if (rawPoints == null || rawPoints.size() < 2) {
            return;
        }

        List<TrackPoint> points = new ArrayList<>(rawPoints.size());
        for (String raw : rawPoints) {
            TrackPoint parsed = parsePoint(raw);
            if (parsed != null) {
                if (points.isEmpty()) {
                    points.add(parsed);
                } else {
                    TrackPoint prev = points.get(points.size() - 1);
                    if (Double.compare(prev.longitude(), parsed.longitude()) != 0
                            || Double.compare(prev.latitude(), parsed.latitude()) != 0) {
                        points.add(parsed);
                    }
                }
            }
        }

        if (points.size() < 2) {
            return;
        }

        Coordinate[] coordinates = new Coordinate[points.size()];
        double totalDistance = 0.0;
        for (int i = 0; i < points.size(); i++) {
            TrackPoint p = points.get(i);
            coordinates[i] = new Coordinate(p.longitude(), p.latitude());
            if (i > 0) {
                TrackPoint prev = points.get(i - 1);
                totalDistance += haversineMeters(prev.latitude(), prev.longitude(), p.latitude(), p.longitude());
            }
        }

        LineString lineString = GEOMETRY_FACTORY.createLineString(coordinates);
        lineString.setSRID(4326);

        LocalDateTime startedAt = toLocalDateTime(points.get(0).epochMs());
        LocalDateTime endedAt = toLocalDateTime(points.get(points.size() - 1).epochMs());

        TeamLocationLog logEntity = TeamLocationLog.builder()
                .campaignTeamId(campaignTeamId)
                .assignmentId(assignmentId)
                .lineString(lineString)
                .distanceMeters(totalDistance)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .build();

        teamLocationLogRepository.saveAndFlush(logEntity);
    }

    private String tryAcquireLock(String lockKey) {
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, token, FLUSH_LOCK_TTL);
            return Boolean.TRUE.equals(acquired) ? token : null;
        } catch (Exception ex) {
            log.warn("Không thể tạo Redis lock {}: {}", lockKey, ex.getMessage());
            return null;
        }
    }

    private String acquireLockWithRetry(String lockKey) {
        String token = UUID.randomUUID().toString();
        for (int attempt = 0; attempt < FINISH_LOCK_MAX_RETRIES; attempt++) {
            try {
                Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, token, FLUSH_LOCK_TTL);
                if (Boolean.TRUE.equals(acquired)) {
                    return token;
                }
            } catch (Exception ex) {
                log.warn("Lỗi Redis khi chờ lock {}: {}", lockKey, ex.getMessage());
                return null;
            }
            if (attempt + 1 < FINISH_LOCK_MAX_RETRIES) {
                try {
                    Thread.sleep(FINISH_LOCK_RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }

    private void releaseLock(String lockKey, String lockToken) {
        try {
            stringRedisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(lockKey), lockToken);
        } catch (Exception ex) {
            log.warn("Không thể giải phóng Redis lock {}: {}", lockKey, ex.getMessage());
        }
    }

    private String bufferKey(UUID teamId, UUID assignmentId) {
        return BUFFER_KEY_PREFIX + teamId + ":" + assignmentId;
    }

    private String encodePoint(double longitude, double latitude, long epochMs) {
        return longitude + "," + latitude + "," + epochMs;
    }

    private TrackPoint parsePoint(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.split(",");
        if (parts.length != 3) {
            return null;
        }
        try {
            double lng = Double.parseDouble(parts[0]);
            double lat = Double.parseDouble(parts[1]);
            long epochMs = Long.parseLong(parts[2]);
            if (!isValidCoordinate(lat, lng)) {
                return null;
            }
            return new TrackPoint(lng, lat, epochMs);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isValidCoordinate(Double latitude, Double longitude) {
        return latitude != null && longitude != null
                && Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90.0 && latitude <= 90.0
                && longitude >= -180.0 && longitude <= 180.0;
    }

    private LocalDateTime toLocalDateTime(long epochMs) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), VN_ZONE);
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private record TrackPoint(double longitude, double latitude, long epochMs) {
    }
}
