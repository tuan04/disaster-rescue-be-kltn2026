package iuh.fit.resourcemanagementservice.dtos.request;

import iuh.fit.resourcemanagementservice.enums.TeamStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTeamStatusRequest(
        @NotNull(message = "Trạng thái đội không được để trống")
        TeamStatus status
) {
}
