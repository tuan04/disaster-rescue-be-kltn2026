package iuh.fit.notificationservice.dtos;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record NotificationResponse(
        UUID id,
        UUID notificationId,
        UUID referenceId,
        String type,
        String title,
        String content,
        Boolean isRead,
        LocalDateTime createdAt
) {
}
