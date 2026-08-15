package iuh.fit.dispatchservice.dtos.response;

import iuh.fit.dispatchservice.enums.PointType;

import java.util.UUID;

public record MapPointDetailResponse (
        UUID id,
        PointType pointType,
        double latitude,
        double longitude,
        MapPointDetailResInterface detail
){
}
