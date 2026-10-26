package iuh.fit.resourcemanagementservice.mappers;

import iuh.fit.resourcemanagementservice.dtos.response.TeamResponse;
import iuh.fit.resourcemanagementservice.dtos.response.TeamSummaryResponse;
import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CampaignTeamMapper {
    @Mapping(target = "campaignId", source = "campaign.id")
    @Mapping(target = "campaignName", source = "campaign.name")
    TeamResponse toResponse(CampaignTeam team);

    @Mapping(target = "campaignName", source = "campaign.name")
    TeamSummaryResponse toSummaryResponse(CampaignTeam team);
}

