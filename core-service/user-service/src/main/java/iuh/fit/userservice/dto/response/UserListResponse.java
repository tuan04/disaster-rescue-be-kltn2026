package iuh.fit.userservice.dto.response;

import iuh.fit.userservice.entity.User;
import iuh.fit.userservice.entity.VolunteerProfile;
import iuh.fit.userservice.enums.RoleEnum;
import iuh.fit.userservice.enums.RoleInTeamEnum;
import iuh.fit.userservice.enums.VerifiedStatusEnum;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserListResponse(
        UUID id,
        String phone,
        String fullName,
        String avatarUrl,
        RoleEnum role,
        String cccdNumber,
        String address,
        String description,
        RoleInTeamEnum currentRoleInTeam,
        UUID teamId,
        String teamName,
        VerifiedStatusEnum verifiedStatus,
        boolean isValidated,
        boolean isDeleted,
        LocalDateTime createdAt
) {

    public static UserListResponse fromEntity(User user) {
        VolunteerProfile profile = user.getVolunteerProfile();

        return new UserListResponse(
                user.getId(),
                user.getPhone(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getRole(),
                profile != null ? profile.getCccdNumber() : null,
                profile != null ? profile.getAddress() : null,
                profile != null ? profile.getDescription() : null,
                profile != null ? profile.getCurrentRoleInTeam() : null,
                profile != null ? profile.getTeamId() : null,
                profile != null ? profile.getTeamName() : null,
                profile != null ? profile.getVerifiedStatus() : null,
                user.isValidated(),
                user.isDeleted(),
                user.getCreatedAt()
        );
    }
}
