package iuh.fit.userservice.dto.request;

import iuh.fit.userservice.enums.AccountRole;
import iuh.fit.userservice.enums.SexEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record AdminUserRequest(
        @NotBlank
        @Pattern(regexp = "^0[35789][0-9]{8}$")
        String phone,

        @NotBlank
        @Size(max = 100)
        String fullName,

        @Size(min = 8, max = 50)
        String password,

        @NotNull
        SexEnum sex,

        @Size(max = 255)
        String avatarUrl,

        @Past
        LocalDate birthDate,

        @NotNull
        AccountRole role,

        @NotNull
        Boolean deleted,

        @NotNull
        Boolean validated,

        @Valid
        AdminVolunteerProfileRequest volunteerProfile
) {
}
