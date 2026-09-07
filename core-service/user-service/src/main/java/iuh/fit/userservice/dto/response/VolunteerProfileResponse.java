package iuh.fit.userservice.dto.response;

import iuh.fit.userservice.entity.VolunteerProfile;
import iuh.fit.userservice.enums.RoleInTeamEnum;
import iuh.fit.userservice.enums.VerifiedStatusEnum;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerProfileResponse {
    private String cccdNumber;
    private UUID teamId;
    private String teamName;
    private String description;
    private RoleInTeamEnum currentRoleInTeam;
    private VerifiedStatusEnum verifiedStatus;

    public static VolunteerProfileResponse fromEntity(VolunteerProfile volunteerProfile) {
        if (volunteerProfile == null) {
            return null;
        }
        return VolunteerProfileResponse.builder()
                .cccdNumber(volunteerProfile.getCccdNumber())
                .teamId(volunteerProfile.getTeamId())
                .teamName(volunteerProfile.getTeamName())
                .description(volunteerProfile.getDescription())
                .currentRoleInTeam(volunteerProfile.getCurrentRoleInTeam())
                .verifiedStatus(volunteerProfile.getVerifiedStatus())
                .build();
    }
}
