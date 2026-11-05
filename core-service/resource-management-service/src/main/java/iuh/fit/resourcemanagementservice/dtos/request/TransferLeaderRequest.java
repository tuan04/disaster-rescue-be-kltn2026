package iuh.fit.resourcemanagementservice.dtos.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TransferLeaderRequest(
        @NotNull(message = "ID của Đội trưởng mới không được để trống")
        UUID newLeaderId
) {
}

