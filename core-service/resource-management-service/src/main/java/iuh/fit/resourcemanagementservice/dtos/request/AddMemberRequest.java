package iuh.fit.resourcemanagementservice.dtos.request;

import iuh.fit.resourcemanagementservice.enums.SexEnum;
import iuh.fit.resourcemanagementservice.enums.TeamRole;
import iuh.fit.resourcemanagementservice.validations.ValidMember;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@ValidMember
public record AddMemberRequest(
        UUID memberId,

        @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
        String fullName,

        @Pattern(regexp = "^0[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại không hợp lệ")
        String phone,

        @NotNull(message = "Vai trò không được để trống")
        TeamRole roleInTeam,

        SexEnum sex
) {
}

