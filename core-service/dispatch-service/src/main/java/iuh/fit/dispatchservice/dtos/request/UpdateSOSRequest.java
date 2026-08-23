package iuh.fit.dispatchservice.dtos.request;

import iuh.fit.dispatchservice.enums.RequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;



@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdateSOSRequest {
    @NotNull(message = "ID sự kiện SOS không được để trống")
    private UUID id;

    @Pattern(regexp = "^0[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại không hợp lệ")
    private String reporterPhone;

    @Size(max = 1000, message = "Nội dung mô tả không được vượt quá 1000 ký tự")
    private String content;

    private RequestStatus status;
}
