package iuh.fit.resourcemanagementservice.dtos.request;

import iuh.fit.resourcemanagementservice.enums.ItemUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateItemRequest(
        @NotBlank(message = "Tên vật phẩm không được để trống")
        @Size(max = 255, message = "Tên vật phẩm không được vượt quá 255 ký tự")
        String name,

        @NotNull(message = "Đơn vị tính không được để trống")
        ItemUnit unit,

        String imageUrl
) {
}
