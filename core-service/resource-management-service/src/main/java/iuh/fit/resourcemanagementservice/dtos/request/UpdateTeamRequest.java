package iuh.fit.resourcemanagementservice.dtos.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UpdateTeamRequest(
        List<String> vehicles,

        @Size(max = 255, message = "Tên đội không được vượt quá 255 ký tự")
        String teamName,

        UUID leaderId,

        @Pattern(regexp = "^0[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại không hợp lệ")
        String leaderPhone
) {
}
