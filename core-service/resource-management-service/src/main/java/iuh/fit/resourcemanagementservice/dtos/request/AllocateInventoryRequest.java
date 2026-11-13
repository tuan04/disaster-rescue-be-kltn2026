package iuh.fit.resourcemanagementservice.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AllocateInventoryRequest(
        @NotNull(message = "warehouseId không được để trống")
        UUID warehouseId,

        @NotNull(message = "campaignTeamId không được để trống")
        UUID campaignTeamId,

        @NotEmpty(message = "Danh sách vật phẩm cấp phát không được để trống")
        @Valid
        List<ItemAllocation> items
) {
    public record ItemAllocation(
            UUID warehouseInventoryId,

            @NotNull(message = "itemId không được để trống")
            UUID itemId,

            @NotNull(message = "Số lượng cấp phát không được để trống")
            @Min(value = 1, message = "Số lượng cấp phát tối thiểu là 1")
            Integer quantity
    ) {}
}
