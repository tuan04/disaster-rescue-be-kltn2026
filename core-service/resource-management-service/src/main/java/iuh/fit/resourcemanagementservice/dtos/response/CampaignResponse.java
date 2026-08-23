package iuh.fit.resourcemanagementservice.dtos.response;

import iuh.fit.resourcemanagementservice.entity.Campaign;
import iuh.fit.resourcemanagementservice.enums.CampaignStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record CampaignResponse(
        UUID id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        CampaignStatus status,
        String province
) {
    public static CampaignResponse fromEntity(Campaign campaign) {
        return CampaignResponse.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .status(campaign.getStatus())
                .province(campaign.getProvince())
                .build();
    }
}
