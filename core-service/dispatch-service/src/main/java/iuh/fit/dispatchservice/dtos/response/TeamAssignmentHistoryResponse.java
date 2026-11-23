package iuh.fit.dispatchservice.dtos.response;

import iuh.fit.dispatchservice.enums.AssignmentStatus;
import iuh.fit.dispatchservice.enums.EmergencyLevel;

import java.time.LocalDateTime;
import java.util.UUID;

public record TeamAssignmentHistoryResponse(
        UUID assignmentId,
        String requestCode,
        AssignmentStatus status,
        EmergencyLevel emergencyLevel,
        String address,
        String content,
        String reporterPhone,
        Double targetLatitude,
        Double targetLongitude,
        Double distanceMeters,
        Long durationMinutes,
        LocalDateTime assignedAt,
        LocalDateTime respondedAt,
        LocalDateTime completedAt,
        String notes
) {}
