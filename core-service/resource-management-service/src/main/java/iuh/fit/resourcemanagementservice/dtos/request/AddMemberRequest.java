package iuh.fit.resourcemanagementservice.dtos.request;

import iuh.fit.resourcemanagementservice.enums.TeamRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddMemberRequest(
        @NotNull(message = "ID thành viên (memberId) không được để trống")
        UUID memberId,

        @NotNull(message = "Vai trò trong đội (roleInTeam) không được để trống")
        TeamRole roleInTeam
) {
}
