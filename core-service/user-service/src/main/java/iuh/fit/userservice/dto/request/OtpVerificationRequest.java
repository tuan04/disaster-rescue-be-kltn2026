package iuh.fit.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class OtpVerificationRequest {
    @NotNull(message = "ID is required")
    private UUID id;
    @NotBlank(message = "Otp number is required")
    @Size(min = 6, max = 6, message = "Otp number must be 6 digits")
    private String otp;
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại không hợp lệ")
    private String phoneNumber;
}
