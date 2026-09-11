package iuh.fit.resourcemanagementservice.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record UpdateCampaignWarehouseInventoryRequest(
        @Min(value = 0, message = "Số lượng không được âm")
        Integer quantity,
        UUID warehouseId,
        @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không hợp lệ")
        String phone,
        Boolean isDeleted
) {
}
