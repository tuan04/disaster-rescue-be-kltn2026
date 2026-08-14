package iuh.fit.dispatchservice.dtos.response;

import iuh.fit.dispatchservice.enums.HazardStatus;
import iuh.fit.dispatchservice.enums.HazardType;

import java.util.List;
import java.util.UUID;

public record HazardDetailResponse(
        UUID id,
        HazardType hazardType,
        String description,
        List<String> imageUrls,
        HazardStatus status
) implements MapPointDetailResInterface {}
