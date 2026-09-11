package iuh.fit.resourcemanagementservice.dtos.response;

import iuh.fit.resourcemanagementservice.entity.TeamMobileInventory;
import iuh.fit.resourcemanagementservice.enums.ItemUnit;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TeamMobileInventoryResponse(
        UUID id,
        UUID campaignTeamId,
        String teamName,
        UUID itemId,
        String itemName,
        ItemUnit itemUnit,
        Integer currentQuantity,
        Boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
    public static TeamMobileInventoryResponse fromEntity(TeamMobileInventory inventory) {
        return TeamMobileInventoryResponse.builder()
                .id(inventory.getId())
                .campaignTeamId(inventory.getCampaignTeam() != null ? inventory.getCampaignTeam().getId() : null)
                .teamName(inventory.getCampaignTeam() != null ? inventory.getCampaignTeam().getTeamName() : null)
                .itemId(inventory.getItem() != null ? inventory.getItem().getId() : null)
                .itemName(inventory.getItem() != null ? inventory.getItem().getName() : null)
                .itemUnit(inventory.getItem() != null ? inventory.getItem().getUnit() : null)
                .currentQuantity(inventory.getCurrentQuantity())
                .isDeleted(inventory.getIsDeleted())
                .createdAt(inventory.getCreatedAt())
                .modifiedAt(inventory.getModifiedAt())
                .build();
    }
}
