package iuh.fit.resourcemanagementservice.dtos.response;

import iuh.fit.resourcemanagementservice.entity.CampaignWarehouseInventory;
import iuh.fit.resourcemanagementservice.enums.ItemUnit;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CampaignWarehouseInventoryResponse(
        UUID id,
        UUID warehouseId,
        UUID itemId,
        String itemName,
        String itemImageUrl,
        ItemUnit itemUnit,
        Integer quantity,
        Boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
    public static CampaignWarehouseInventoryResponse fromEntity(CampaignWarehouseInventory inventory) {
        return CampaignWarehouseInventoryResponse.builder()
                .id(inventory.getId())
                .warehouseId(inventory.getWarehouseId())
                .itemId(inventory.getItem() != null ? inventory.getItem().getId() : null)
                .itemName(inventory.getItem() != null ? inventory.getItem().getName() : null)
                .itemImageUrl(inventory.getItem() != null ? inventory.getItem().getImageUrl() : null)
                .itemUnit(inventory.getItem() != null ? inventory.getItem().getUnit() : null)
                .quantity(inventory.getQuantity())
                .isDeleted(inventory.getIsDeleted())
                .createdAt(inventory.getCreatedAt())
                .modifiedAt(inventory.getModifiedAt())
                .build();
    }
}
