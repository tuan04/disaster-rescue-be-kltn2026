package iuh.fit.dispatchservice.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateWarehouseRequest {
    @Pattern(regexp = "^0[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại quản lý không hợp lệ")
    private String managerPhone;

    @NotBlank(message = "Tên kho không được để trống")
    private String name;

    @NotNull(message = "Thông tin điểm bản đồ không được để trống")
    @Valid
    private MapPointRequest mapPoint;
}

