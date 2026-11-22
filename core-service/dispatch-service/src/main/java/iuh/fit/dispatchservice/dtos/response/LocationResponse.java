package iuh.fit.dispatchservice.dtos.response;

import java.util.UUID;

public record LocationResponse(
        UUID id,
        String code,
        String name
) {
}
