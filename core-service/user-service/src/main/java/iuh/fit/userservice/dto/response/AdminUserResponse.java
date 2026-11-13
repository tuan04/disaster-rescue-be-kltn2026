package iuh.fit.userservice.dto.response;

import iuh.fit.userservice.dto.request.AdminVolunteerProfileRequest;
import iuh.fit.userservice.entity.User;
import iuh.fit.userservice.entity.VolunteerProfile;
import iuh.fit.userservice.enums.AccountRole;
import iuh.fit.userservice.enums.SexEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;



public record AdminUserResponse(
        UUID id,
        String phone,
        String fullName,
        SexEnum sex,
        String avatarUrl,
        LocalDate birthDate,
        AccountRole role,
        boolean deleted,
        boolean validated,
        AdminVolunteerProfileRequest volunteerProfile,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
    public static AdminUserResponse fromEntity(User user) {
        VolunteerProfile profile = user.getVolunteerProfile();

        AdminVolunteerProfileRequest profileData =
                profile == null ? null :
                        new AdminVolunteerProfileRequest(
                                profile.getCccdNumber(),
                                profile.getDescription(),
                                profile.getAddress(),
                                profile.getVerifiedStatus()
                        );

        return new AdminUserResponse(
                user.getId(),
                user.getPhone(),
                user.getFullName(),
                user.getSex(),
                user.getAvatarUrl(),
                user.getBirthDate(),
                AccountRole.valueOf(user.getEffectiveRole()),
                user.isDeleted(),
                user.isValidated(),
                profileData,
                user.getCreatedAt(),
                user.getModifiedAt()
        );
    }
}
