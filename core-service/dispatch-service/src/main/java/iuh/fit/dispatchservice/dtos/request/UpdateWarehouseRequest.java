package iuh.fit.dispatchservice.dtos.request;

import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateWarehouseRequest {
    private UUID id;

    private String name;

    @Pattern(regexp = "^0[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại quản lý không hợp lệ")
    private String managerPhone;

    private Boolean isActive;
}
