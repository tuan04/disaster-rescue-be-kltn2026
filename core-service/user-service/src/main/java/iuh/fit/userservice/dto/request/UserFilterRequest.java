package iuh.fit.userservice.dto.request;

import iuh.fit.userservice.enums.RoleEnum;
import iuh.fit.userservice.enums.RoleInTeamEnum;
import iuh.fit.userservice.enums.VerifiedStatusEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


@Getter
@Setter
public class UserFilterRequest {
    private String keyword;

    private RoleInTeamEnum currentRoleInTeam;

    private RoleEnum role;

    private UUID teamId;

    private VerifiedStatusEnum verifiedStatus;

    private Boolean isValidated;
}
