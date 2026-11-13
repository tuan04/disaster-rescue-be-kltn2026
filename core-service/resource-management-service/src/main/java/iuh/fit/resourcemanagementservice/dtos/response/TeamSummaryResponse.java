package iuh.fit.resourcemanagementservice.dtos.response;

import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.enums.TeamStatus;

import java.util.List;
import java.util.UUID;

public record TeamSummaryResponse(
        UUID id,
        String teamName,
        String leaderName,
        String leaderPhone,
        List<String> vehicles,
        String campaignName,
        TeamStatus status
) {
    public static TeamSummaryResponse fromEntity(CampaignTeam team) {
        return new TeamSummaryResponse(
                team.getId(),
                team.getTeamName(),
                team.getLeaderName(),
                team.getLeaderPhone(),
                team.getVehicles(),
                team.getCampaign() != null ? team.getCampaign().getName() : null,
                team.getStatus()
        );
    }
}
