package iuh.fit.dispatchservice.dtos.request;

import iuh.fit.dispatchservice.enums.SafePointType;
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
public class CreateSafePointRequest {
    @NotBlank(message = "Tên điểm an toàn không được để trống")
    private String name;

    @NotNull(message = "Loại điểm an toàn không được để trống")
    private SafePointType safePointType;

    @Pattern(regexp = "^0[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại liên hệ không hợp lệ")
    private String contactPhone;

    @NotNull(message = "Thông tin điểm bản đồ không được để trống")
    @Valid
    private MapPointRequest mapPoint;
}

