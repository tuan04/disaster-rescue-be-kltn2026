package iuh.fit.resourcemanagementservice.mappers;

import iuh.fit.resourcemanagementservice.dtos.request.AddMemberRequest;
import iuh.fit.resourcemanagementservice.dtos.response.MemberResponse;
import iuh.fit.resourcemanagementservice.entity.CampaignMember;
import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.enums.SexEnum;
import iuh.fit.resourcemanagementservice.enums.TeamRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring", imports = {LocalDateTime.class, TeamRole.class})
public interface CampaignMemberMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "campaignTeam", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "fullName", source = "fullName", qualifiedByName = "trimOrFallback")
    @Mapping(target = "phone", source = "phone", qualifiedByName = "trimString")
    @Mapping(target = "roleInTeam", source = "roleInTeam", defaultExpression = "java(TeamRole.MEMBER)")
    @Mapping(target = "membershipStatus", ignore = true)
    @Mapping(target = "membershipType", ignore = true)
    CampaignMember toEntity(AddMemberRequest request);

    MemberResponse toResponse(CampaignMember member);

    default CampaignMember toLeaderMember(CampaignTeam team, java.util.UUID leaderId, String phone, String fullName, String avatarUrl, SexEnum sex) {
        return CampaignMember.builder()
                .campaignTeam(team)
                .memberId(leaderId)
                .phone(phone != null ? phone.trim() : null)
                .fullName(fullName != null ? fullName.trim() : null)
                .avatarUrl(avatarUrl != null && !avatarUrl.isBlank() ? avatarUrl : null)
                .roleInTeam(TeamRole.LEADER)
                .sex(sex)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Named("trimOrFallback")
    default String trimOrFallback(String value) {
        if (value == null || value.isBlank()) {
            return "Thành viên";
        }
        return value.trim();
    }

    @Named("trimString")
    default String trimString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
