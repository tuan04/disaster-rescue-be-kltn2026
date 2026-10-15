package iuh.fit.resourcemanagementservice.dtos.response;

import iuh.fit.resourcemanagementservice.entity.CampaignMember;
import iuh.fit.resourcemanagementservice.enums.TeamRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record TeamInvitationResponse(
        UUID membershipId,
        UUID teamId,
        String teamName,
        UUID campaignId,
        String campaignName,
        String leaderName,
        String leaderPhone,
        TeamRole roleInTeam,
        LocalDateTime createdAt
) {
    public static TeamInvitationResponse fromEntity(CampaignMember member) {
        var team = member.getCampaignTeam();
        return new TeamInvitationResponse(
                member.getId(),
                team.getId(),
                team.getTeamName(),
                team.getCampaign() != null ? team.getCampaign().getId() : null,
                team.getCampaign() != null ? team.getCampaign().getName() : null,
                team.getLeaderName(),
                team.getLeaderPhone(),
                member.getRoleInTeam(),
                member.getCreatedAt()
        );
    }
}
