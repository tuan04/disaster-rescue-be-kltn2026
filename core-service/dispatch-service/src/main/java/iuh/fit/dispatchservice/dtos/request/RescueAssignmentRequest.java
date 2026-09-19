package iuh.fit.dispatchservice.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RescueAssignmentRequest(
        @NotNull(message = "Campaign Team ID không được để trống")
        UUID campaignTeamId,

        @NotBlank(message = "Note không được để trống")
        String note,

        @NotBlank(message = "Team name không được để trống")
        String teamName,

        @NotBlank(message = "Leader phone không được để trống")
        String leaderPhone
) {
}