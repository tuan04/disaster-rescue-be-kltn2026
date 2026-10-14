package iuh.fit.resourcemanagementservice.mappers;

import iuh.fit.resourcemanagementservice.dtos.response.MemberResponse;
import iuh.fit.resourcemanagementservice.entity.CampaignMember;
import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.enums.MembershipStatus;
import iuh.fit.resourcemanagementservice.enums.MembershipType;
import iuh.fit.resourcemanagementservice.enums.SexEnum;
import iuh.fit.resourcemanagementservice.enums.TeamRole;
import org.mapstruct.Mapper;

import java.time.LocalDateTime;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CampaignMemberMapper {

    MemberResponse toResponse(CampaignMember member);

    default CampaignMember toMember(
            CampaignTeam team,
            UUID memberId,
            String fullName,
            String phone,
            String avatarUrl,
            SexEnum sex,
            TeamRole roleInTeam,
            MembershipType membershipType,
            MembershipStatus membershipStatus
    ) {
        return CampaignMember.builder()
                .campaignTeam(team)
                .memberId(memberId)
                .fullName(fullName != null ? fullName.trim() : null)
                .phone(phone != null ? phone.trim() : null)
                .avatarUrl(avatarUrl != null && !avatarUrl.isBlank() ? avatarUrl : null)
                .sex(sex)
                .roleInTeam(roleInTeam != null ? roleInTeam : TeamRole.MEMBER)
                .membershipType(membershipType != null ? membershipType : (memberId == null ? MembershipType.GUEST : MembershipType.REGISTERED))
                .membershipStatus(membershipStatus != null ? membershipStatus : (memberId == null ? MembershipStatus.ACCEPTED : MembershipStatus.INVITED))
                .createdAt(LocalDateTime.now())
                .build();
    }

    default CampaignMember updateMember(
            CampaignMember existing,
            String fullName,
            String phone,
            String avatarUrl,
            SexEnum sex,
            TeamRole roleInTeam,
            MembershipStatus membershipStatus
    ) {
        existing.setFullName(fullName != null ? fullName.trim() : existing.getFullName());
        existing.setPhone(phone != null ? phone.trim() : existing.getPhone());
        existing.setAvatarUrl(avatarUrl != null && !avatarUrl.isBlank() ? avatarUrl : null);
        existing.setSex(sex);
        existing.setRoleInTeam(roleInTeam != null ? roleInTeam : TeamRole.MEMBER);
        existing.setMembershipStatus(membershipStatus != null ? membershipStatus : existing.getMembershipStatus());
        existing.setModifiedAt(LocalDateTime.now());
        return existing;
    }
}
