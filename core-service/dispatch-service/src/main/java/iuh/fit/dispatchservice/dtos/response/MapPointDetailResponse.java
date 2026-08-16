package iuh.fit.dispatchservice.dtos.response;

import iuh.fit.dispatchservice.enums.PointType;

import java.time.LocalDateTime;
import java.util.UUID;

public record MapPointDetailResponse (
        UUID id,
        PointType pointType,
        double latitude,
        double longitude,
        String address,
        LocalDateTime createdAt,
        MapPointDetailResInterface detail
){
}
