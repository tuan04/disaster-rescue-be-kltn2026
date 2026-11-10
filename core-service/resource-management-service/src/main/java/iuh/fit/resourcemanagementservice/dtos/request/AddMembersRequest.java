package iuh.fit.resourcemanagementservice.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AddMembersRequest(
        @NotEmpty(message = "Danh sách thành viên không được để trống")
        List<@Valid AddMemberRequest> members
) {
    @AssertTrue(message = "Danh sách yêu cầu chứa ID người dùng trùng lặp")
    public boolean isUniqueMemberIds() {
        if (members == null) return true;
        Set<UUID> ids = new HashSet<>();
        for (AddMemberRequest m : members) {
            if (m != null && m.memberId() != null && !ids.add(m.memberId())) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(message = "Danh sách yêu cầu chứa số điện thoại trùng lặp")
    public boolean isUniqueGuestPhones() {
        if (members == null) return true;
        Set<String> phones = new HashSet<>();
        for (AddMemberRequest m : members) {
            if (m != null && m.memberId() == null && m.phone() != null && !m.phone().isBlank()) {
                if (!phones.add(m.phone().trim())) {
                    return false;
                }
            }
        }
        return true;
    }
}
