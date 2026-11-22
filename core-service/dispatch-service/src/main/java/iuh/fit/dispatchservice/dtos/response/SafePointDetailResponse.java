package iuh.fit.dispatchservice.dtos.response;

import iuh.fit.dispatchservice.enums.SafePointType;

import java.util.UUID;

public record SafePointDetailResponse(
        UUID id,
        String code,
        String name,
        String contactPhone,
        SafePointType safePointType,
        Boolean isActive
) implements MapPointDetailResInterface {
}
