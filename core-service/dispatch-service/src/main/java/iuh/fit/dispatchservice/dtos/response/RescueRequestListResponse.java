package iuh.fit.dispatchservice.dtos.response;

import iuh.fit.dispatchservice.entity.MapPoint;
import iuh.fit.dispatchservice.entity.RescueRequest;
import iuh.fit.dispatchservice.enums.EmergencyLevel;
import iuh.fit.dispatchservice.enums.RequestSource;
import iuh.fit.dispatchservice.enums.RequestStatus;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.UUID;
public record RescueRequestListResponse(
        UUID id,
        UUID reporterId,
        String reporterPhone,
        String content,
        EmergencyLevel emergencyLevel,
        RequestStatus status,
        RequestSource source,

        // Thông tin lấy từ MapPoint.
        UUID locationId,
        String address,
        Double latitude,
        Double longitude,

        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        LocalDateTime completeAt,
        String aiEvaluation
) {

    public static RescueRequestListResponse from(RescueRequest request) {

        // MapPoint được repository tải kèm nhờ @EntityGraph hiện có.
        MapPoint mapPoint = request.getMapPoint();

        // Kiểm tra null để trả được cả dữ liệu chưa có tọa độ.
        Point point = mapPoint == null
                ? null
                : mapPoint.getLocation();

        boolean hasCoordinates = point != null && !point.isEmpty();

        return new RescueRequestListResponse(
                request.getId(),
                request.getReporterId(),
                request.getReporterPhone(),
                request.getContent(),
                request.getEmergencyLevel(),
                request.getStatus(),
                request.getSource(),

                mapPoint == null ? null : mapPoint.getLocationId(),
                mapPoint == null ? null : mapPoint.getAddress(),

                // JTS: X là kinh độ, Y là vĩ độ.
                hasCoordinates ? point.getY() : null,
                hasCoordinates ? point.getX() : null,

                request.getCreatedAt(),
                request.getModifiedAt(),
                request.getCompleteAt(),
                request.getAiEvaluation()
        );
    }
}
