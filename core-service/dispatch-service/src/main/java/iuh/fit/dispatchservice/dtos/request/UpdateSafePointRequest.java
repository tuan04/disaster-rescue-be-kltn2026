package iuh.fit.dispatchservice.dtos.request;

import iuh.fit.dispatchservice.enums.SafePointType;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateSafePointRequest {
    private UUID id;

    private String name;

    private SafePointType safePointType;

    @Pattern(regexp = "^0[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại liên hệ không hợp lệ")
    private String contactPhone;

    private Boolean isActive;
}
