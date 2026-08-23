package iuh.fit.resourcemanagementservice.dtos.response;

import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.enums.TeamStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TeamResponse(
        UUID id,
        UUID campaignId,
        Integer totalParticipants,
        TeamStatus status,
        LocalDateTime createdAt,
        List<String> vehicles,
        String teamName,
        UUID leaderId,
        String leaderPhone
) {
    public static TeamResponse fromEntity(CampaignTeam team) {
        return new TeamResponse(
                team.getId(),
                team.getCampaignId(),
                team.getTotalParticipants(),
                team.getStatus(),
                team.getCreatedAt(),
                team.getVehicles(),
                team.getTeamName(),
                team.getLeaderId(),
                team.getLeaderPhone()
        );
    }
}
