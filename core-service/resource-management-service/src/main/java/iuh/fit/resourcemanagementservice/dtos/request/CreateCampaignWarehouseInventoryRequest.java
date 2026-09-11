package iuh.fit.resourcemanagementservice.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateCampaignWarehouseInventoryRequest(
        @NotNull(message = "campaignId không được để trống")
        UUID campaignId,

        @NotNull(message = "warehouseId không được để trống")
        UUID warehouseId,

        @NotNull(message = "itemId không được để trống")
        UUID itemId,

        @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không hợp lệ")
        String phone,

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 0, message = "Số lượng không được âm")
        Integer quantity
) {
}
