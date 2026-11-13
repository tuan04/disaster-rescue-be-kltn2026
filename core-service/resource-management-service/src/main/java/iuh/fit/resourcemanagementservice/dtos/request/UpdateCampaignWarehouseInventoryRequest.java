package iuh.fit.resourcemanagementservice.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCampaignWarehouseInventoryRequest(
        @NotNull (message = "Số lượng không được để trống")
        @Min(value = 0, message = "Số lượng không được âm")
        Integer quantity,
        
        Boolean isDeleted
) {
}
