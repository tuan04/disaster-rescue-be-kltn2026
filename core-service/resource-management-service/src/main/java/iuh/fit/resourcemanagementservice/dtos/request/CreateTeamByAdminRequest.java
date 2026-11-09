package iuh.fit.resourcemanagementservice.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateTeamByAdminRequest(
        @NotNull(message = "campaignId không được để trống")
        UUID campaignId,

        @NotBlank(message = "Tên đội không được để trống")
        @Size(max = 255, message = "Tên đội không được vượt quá 255 ký tự")
        String teamName,

        @NotNull(message = "leaderId không được để trống")
        UUID leaderId,

        List<String> vehicles
) {
}
