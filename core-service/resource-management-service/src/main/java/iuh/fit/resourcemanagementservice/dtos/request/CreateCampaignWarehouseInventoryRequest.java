package iuh.fit.resourcemanagementservice.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCampaignWarehouseInventoryRequest(
        @NotNull(message = "warehouseId không được để trống")
        UUID warehouseId,

        @NotNull(message = "itemId không được để trống")
        UUID itemId,

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 0, message = "Số lượng không được âm")
        Integer quantity
) {
}
