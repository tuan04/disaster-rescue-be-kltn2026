package iuh.fit.resourcemanagementservice.dtos.response;

import iuh.fit.resourcemanagementservice.entity.CampaignMember;
import iuh.fit.resourcemanagementservice.enums.SexEnum;
import iuh.fit.resourcemanagementservice.enums.TeamRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemberResponse(
        UUID id,
        UUID memberId,
        String fullName,
        String phone,
        TeamRole roleInTeam,
        SexEnum sex,
        String avatarUrl,
        LocalDateTime createdAt
) {
    public static MemberResponse fromEntity(CampaignMember member) {
        return new MemberResponse(
                member.getId(),
                member.getMemberId(),
                member.getFullName(),
                member.getPhone(),
                member.getRoleInTeam(),
                member.getSex(),
                member.getAvatarUrl(),
                member.getCreatedAt()
        );
    }
}
