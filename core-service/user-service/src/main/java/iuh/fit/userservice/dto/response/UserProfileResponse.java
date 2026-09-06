package iuh.fit.userservice.dto.response;

import iuh.fit.userservice.entity.User;
import iuh.fit.userservice.enums.RoleEnum;
import iuh.fit.userservice.enums.SexEnum;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private UUID id;
    private String fullName;
    private String phone;
    private SexEnum sex;
    private String avatarUrl;
    private RoleEnum role;
    private LocalDate birthDate;
    private VolunteerProfileResponse volunteerProfile;

    public static UserProfileResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .sex(user.getSex())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .birthDate(user.getBirthDate())
                .volunteerProfile(VolunteerProfileResponse.fromEntity(user.getVolunteerProfile()))
                .build();
    }
}
