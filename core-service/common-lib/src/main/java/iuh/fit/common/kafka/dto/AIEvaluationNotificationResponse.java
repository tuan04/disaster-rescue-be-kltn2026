package iuh.fit.common.kafka.dto;

import java.util.UUID;

public record AIEvaluationNotificationResponse(
        UUID requestId,
        String emergencyLevel,
        String evaluate,
        String address

) {
}
