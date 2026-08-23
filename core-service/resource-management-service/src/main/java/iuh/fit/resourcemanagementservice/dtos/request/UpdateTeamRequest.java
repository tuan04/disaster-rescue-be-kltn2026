package iuh.fit.resourcemanagementservice.dtos.request;

import iuh.fit.resourcemanagementservice.enums.TeamStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UpdateTeamRequest(
        UUID campaignId,

        @Min(value = 0, message = "Số lượng thành viên không được âm")
        Integer totalParticipants,

        @NotNull(message = "Trạng thái đội không được để trống")
        TeamStatus status,

        List<String> vehicles,

        @Size(max = 255, message = "Tên đội không được vượt quá 255 ký tự")
        String teamName,

        @NotNull(message = "ID trưởng đội (leaderId) không được để trống")
        UUID leaderId,

        @NotBlank(message = "Số điện thoại trưởng đội (leaderPhone) không được để trống")
        @Pattern(regexp = "^0[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại không hợp lệ")
        String leaderPhone
) {
}
