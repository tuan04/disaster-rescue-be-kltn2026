package iuh.fit.resourcemanagementservice.dtos.response;

import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.enums.TeamStatus;
import iuh.fit.resourcemanagementservice.enums.TeamApprovalStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TeamResponse(
        UUID id,
        UUID campaignId,
        String campaignName,
        TeamStatus status,
        LocalDateTime createdAt,
        List<String> vehicles,
        String teamName,
        UUID leaderId,
        String leaderPhone,
        TeamApprovalStatus approvalStatus,
        LocalDateTime requestedAt,
        LocalDateTime reviewedAt,
        UUID reviewedBy,
        String rejectionReason
) {
    public static TeamResponse fromEntity(CampaignTeam team) {
        return new TeamResponse(
                team.getId(),
                team.getCampaign() != null ? team.getCampaign().getId() : null,
                team.getCampaign() != null ? team.getCampaign().getName() : null,
                team.getStatus(),
                team.getCreatedAt(),
                team.getVehicles(),
                team.getTeamName(),
                team.getLeaderId(),
                team.getLeaderPhone(),
                team.getApprovalStatus(),
                team.getRequestedAt(),
                team.getReviewedAt(),
                team.getReviewedBy(),
                team.getRejectionReason()
        );
    }
}
