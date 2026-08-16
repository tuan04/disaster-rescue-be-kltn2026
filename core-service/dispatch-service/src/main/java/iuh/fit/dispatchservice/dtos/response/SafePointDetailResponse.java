package iuh.fit.dispatchservice.dtos.response;

import iuh.fit.dispatchservice.enums.SafePointType;

import java.util.UUID;

public record SafePointDetailResponse(
        UUID id,
        String name,
        String contactPhone,
        SafePointType safePointType,
        Boolean isActive
) implements MapPointDetailResInterface {
}
