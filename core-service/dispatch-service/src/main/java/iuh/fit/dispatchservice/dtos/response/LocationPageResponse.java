package iuh.fit.dispatchservice.dtos.response;

import iuh.fit.dispatchservice.enums.LocationStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record LocationPageResponse(
        UUID id,
        UUID userId,
        String name,
        GeoJsonPolygonDto boundary,
        Integer radiusMeters,
        LocationStatus status,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        String userName,
        String userPhone
) {
}

