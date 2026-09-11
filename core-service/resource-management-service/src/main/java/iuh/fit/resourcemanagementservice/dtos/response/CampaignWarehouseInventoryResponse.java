package iuh.fit.resourcemanagementservice.dtos.response;

import iuh.fit.resourcemanagementservice.entity.CampaignWarehouseInventory;
import iuh.fit.resourcemanagementservice.enums.ItemUnit;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CampaignWarehouseInventoryResponse(
        UUID id,
        UUID campaignId,
        String campaignName,
        UUID warehouseId,
        UUID itemId,
        String itemName,
        ItemUnit itemUnit,
        Integer quantity,
        Boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
    public static CampaignWarehouseInventoryResponse fromEntity(CampaignWarehouseInventory inventory) {
        return CampaignWarehouseInventoryResponse.builder()
                .id(inventory.getId())
                .campaignId(inventory.getCampaign() != null ? inventory.getCampaign().getId() : null)
                .campaignName(inventory.getCampaign() != null ? inventory.getCampaign().getName() : null)
                .warehouseId(inventory.getWarehouseId())
                .itemId(inventory.getItem() != null ? inventory.getItem().getId() : null)
                .itemName(inventory.getItem() != null ? inventory.getItem().getName() : null)
                .itemUnit(inventory.getItem() != null ? inventory.getItem().getUnit() : null)
                .quantity(inventory.getQuantity())
                .isDeleted(inventory.getIsDeleted())
                .createdAt(inventory.getCreatedAt())
                .modifiedAt(inventory.getModifiedAt())
                .build();
    }
}
