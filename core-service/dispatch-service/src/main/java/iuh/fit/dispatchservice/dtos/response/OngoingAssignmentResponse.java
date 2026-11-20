package iuh.fit.dispatchservice.dtos.response;

import iuh.fit.dispatchservice.enums.AssignmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OngoingAssignmentResponse(
        UUID assignmentId,
        UUID requestId,
        UUID campaignTeamId,
        String teamName,
        AssignmentStatus status,
        LocalDateTime updatedAt
) {
}