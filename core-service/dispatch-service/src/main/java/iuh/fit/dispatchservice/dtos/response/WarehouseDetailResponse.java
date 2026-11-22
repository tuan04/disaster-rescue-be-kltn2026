package iuh.fit.dispatchservice.dtos.response;

import java.util.UUID;

public record WarehouseDetailResponse(
        UUID id,
        String code,
        String name,
        String managerPhone,
        Boolean isActive
) implements MapPointDetailResInterface {
}
