package iuh.fit.common.kafka.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TeamApprovalEvent(
        UUID teamId,
        String teamName,
        UUID leaderId,
        String status, // "APPROVED" hoặc "REJECTED"
        String rejectionReason,
        LocalDateTime timestamp
) {
}
