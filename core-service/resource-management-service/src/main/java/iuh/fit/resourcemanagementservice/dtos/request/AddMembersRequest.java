package iuh.fit.resourcemanagementservice.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddMembersRequest(
        @NotEmpty(message = "Danh sách thành viên không được để trống")
        List<@Valid AddMemberRequest> members
) {
}
