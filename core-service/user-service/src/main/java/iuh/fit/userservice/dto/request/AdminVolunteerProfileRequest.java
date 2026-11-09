package iuh.fit.userservice.dto.request;

import iuh.fit.userservice.enums.VerifiedStatusEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminVolunteerProfileRequest(
        @Pattern(regexp = "^[0-9]{12}$")
        String cccdNumber,
        
        @Size(max = 2000)
        String description,

        @Size(max = 1000)
        String address,

        @NotNull
        VerifiedStatusEnum verifiedStatus
) {
}
