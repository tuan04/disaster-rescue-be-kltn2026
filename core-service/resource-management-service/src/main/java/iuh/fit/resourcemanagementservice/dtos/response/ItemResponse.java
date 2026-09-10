package iuh.fit.resourcemanagementservice.dtos.response;

import iuh.fit.resourcemanagementservice.entity.Item;
import iuh.fit.resourcemanagementservice.enums.ItemUnit;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record ItemResponse(
        UUID id,
        String name,
        String imageUrl,
        ItemUnit unit,
        Boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
    public static ItemResponse fromEntity(Item item) {
        return ItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .imageUrl(item.getImageUrl())
                .unit(item.getUnit())
                .isDeleted(item.getIsDeleted())
                .createdAt(item.getCreatedAt())
                .modifiedAt(item.getModifiedAt())
                .build();
    }
}
