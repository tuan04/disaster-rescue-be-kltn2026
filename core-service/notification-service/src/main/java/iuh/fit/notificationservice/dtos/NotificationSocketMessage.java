package iuh.fit.notificationservice.dtos;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record NotificationSocketMessage(
        UUID id,
        UUID referenceId,
        String type,
        String title,
        String content,
        LocalDateTime createdAt,
        double latitude,
        double longitude,
        String emergencyLevel,
        String reporterPhone
) {
}
