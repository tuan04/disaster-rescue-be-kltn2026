package iuh.fit.resourcemanagementservice.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateTeamMobileInventoryRequest(
        @NotNull(message = "id tồn kho không được để trống")
        UUID id,

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 0, message = "Số lượng không được âm")
        Integer currentQuantity,

        Boolean isDeleted
) {
}
