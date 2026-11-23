package iuh.fit.dispatchservice.dtos.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AssignmentTrackResponse(
        UUID assignmentId,
        String requestCode,
        Double targetLatitude,
        Double targetLongitude,
        String targetAddress,
        double totalDistanceMeters,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        List<List<Double>> coordinates
) {}
