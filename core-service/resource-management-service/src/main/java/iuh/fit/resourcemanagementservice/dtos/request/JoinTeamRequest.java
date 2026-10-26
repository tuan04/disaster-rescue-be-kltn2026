package iuh.fit.resourcemanagementservice.dtos.request;

import iuh.fit.resourcemanagementservice.enums.TeamRole;
import iuh.fit.resourcemanagementservice.validations.ValidJoinRole;
import jakarta.validation.constraints.NotNull;

public record JoinTeamRequest(
        @NotNull(message = "Vai trò trong đội không được để trống")
        @ValidJoinRole
        TeamRole roleInTeam
) {
}
