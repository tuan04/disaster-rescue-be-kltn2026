package iuh.fit.resourcemanagementservice.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectTeamRequest(
        @NotBlank(message = "Lý do từ chối không được để trống")
        @Size(max = 255, message = "Lý do từ chối không được vượt quá 255 ký tự")
        String reason
) {
}
