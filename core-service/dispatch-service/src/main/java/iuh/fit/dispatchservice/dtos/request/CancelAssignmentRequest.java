package iuh.fit.dispatchservice.dtos.request;

import jakarta.validation.constraints.NotBlank;

public record CancelAssignmentRequest(
        @NotBlank(message = "Vui lòng nhập lý do hủy")
        String reason
) {
}
