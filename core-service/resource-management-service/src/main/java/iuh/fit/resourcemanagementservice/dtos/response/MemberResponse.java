package iuh.fit.resourcemanagementservice.dtos.response;

import iuh.fit.resourcemanagementservice.entity.CampaignMember;
import iuh.fit.resourcemanagementservice.enums.TeamRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemberResponse(
        UUID id,
        UUID memberId,
        TeamRole roleInTeam,
        LocalDateTime createdAt
) {
    public static MemberResponse fromEntity(CampaignMember member) {
        return new MemberResponse(
                member.getId(),
                member.getMemberId(),
                member.getRoleInTeam(),
                member.getCreatedAt()
        );
    }
}
