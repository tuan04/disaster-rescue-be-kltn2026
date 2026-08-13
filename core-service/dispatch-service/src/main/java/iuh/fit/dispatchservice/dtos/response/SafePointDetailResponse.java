package iuh.fit.dispatchservice.dtos.response;

import java.util.UUID;

public record SafePointDetailResponse(
        UUID id,
        String name,
        Integer maxCapacity,
        Integer currentPeople,
        String contactPhone,
        Boolean isActive
) implements MapPointDetailResInterface { }
