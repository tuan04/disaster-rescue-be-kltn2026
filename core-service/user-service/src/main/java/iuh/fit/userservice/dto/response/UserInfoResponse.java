package iuh.fit.userservice.dto.response;

import iuh.fit.userservice.enums.RoleEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Builder
@Getter
@Setter
public class UserInfoResponse {
    private UUID id;
    private RoleEnum role;
    private String fullName;
    private String phone;
}
