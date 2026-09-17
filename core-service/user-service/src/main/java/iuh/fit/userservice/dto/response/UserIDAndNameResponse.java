package iuh.fit.userservice.dto.response;

import java.util.UUID;

public record UserIDAndNameResponse (
        UUID id,
        String name
) {
}
