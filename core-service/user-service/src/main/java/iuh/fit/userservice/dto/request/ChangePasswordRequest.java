package iuh.fit.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Mật khẩu cũ không được để trống")
        String oldPassword,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(
                min = 8,
                max = 50,
                message = "Mật khẩu mới phải từ 8 đến 50 ký tự"
        )
        String newPassword,

        @NotBlank(message = "Xác nhận mật khẩu mới không được để trống")
        @Size(
                min = 8,
                max = 50,
                message = "Xác nhận mật khẩu mới phải từ 8 đến 50 ký tự"
        )
        String confirmNewPassword
) {
}