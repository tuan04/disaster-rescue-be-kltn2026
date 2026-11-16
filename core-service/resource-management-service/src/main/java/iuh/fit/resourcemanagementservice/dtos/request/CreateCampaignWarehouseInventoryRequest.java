package iuh.fit.resourcemanagementservice.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateCampaignWarehouseInventoryRequest(
        @NotNull(message = "warehouseId không được để trống")
        UUID warehouseId,

        @NotEmpty(message = "Danh sách vật phẩm nhập kho không được để trống")
        @Valid
        List<WarehouseItemImport> items
) {
    public record WarehouseItemImport(
            @NotNull(message = "itemId không được để trống")
            UUID itemId,

            @NotNull(message = "Số lượng không được để trống")
            @Min(value = 0, message = "Số lượng không được âm")
            Integer quantity
    ) {}
}
