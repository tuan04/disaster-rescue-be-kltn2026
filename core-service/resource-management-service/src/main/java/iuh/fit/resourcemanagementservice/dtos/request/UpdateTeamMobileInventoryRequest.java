package iuh.fit.resourcemanagementservice.dtos.request;

import jakarta.validation.constraints.Min;

public record UpdateTeamMobileInventoryRequest(
        @Min(value = 0, message = "Số lượng không được âm")
        Integer currentQuantity,
        Boolean isDeleted
) {
}
