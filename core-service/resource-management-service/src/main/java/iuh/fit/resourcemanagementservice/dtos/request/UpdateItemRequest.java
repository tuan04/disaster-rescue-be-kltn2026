package iuh.fit.resourcemanagementservice.dtos.request;

import iuh.fit.resourcemanagementservice.enums.ItemUnit;
import jakarta.validation.constraints.Size;

public record UpdateItemRequest(
        @Size(max = 255, message = "Tên vật phẩm không được vượt quá 255 ký tự")
        String name,

        ItemUnit unit,

        String imageUrl
) {
}
