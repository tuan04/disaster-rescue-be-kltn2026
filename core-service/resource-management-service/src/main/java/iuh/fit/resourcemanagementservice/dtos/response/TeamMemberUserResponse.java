package iuh.fit.resourcemanagementservice.dtos.response;

import java.util.UUID;

public record TeamMemberUserResponse (
        UUID getTeamId,
        UUID getUserId
) {
}
