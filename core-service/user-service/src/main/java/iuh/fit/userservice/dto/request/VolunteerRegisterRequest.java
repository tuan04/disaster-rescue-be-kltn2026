package iuh.fit.userservice.dto.request;

import iuh.fit.userservice.enums.SexEnum;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class VolunteerRegisterRequest extends CitizenRegisterRequest {
    @NotBlank(message = "Số CCCD không được để trống")
    @Pattern(
            regexp = "^0(0[1-9]|[1-8][0-9]|9[0-6])[0-3][0-9]{8}$",
            message = "Số CCCD không hợp lệ hoặc sai định dạng quốc gia"
    )
    private String cccdNumber;

    public VolunteerRegisterRequest(@NotBlank(message = "Số điện thoại không được để trống") @Pattern(regexp = "^0[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại không hợp lệ") String phone, @NotBlank(message = "Mật khẩu không được để trống") @Size(min = 6, max = 50, message = "Mật khẩu phải từ 6 đến 50 ký tự") String password, @NotBlank(message = "Xác nhận mật khẩu không được để trống") String confirmPassword, @NotBlank(message = "Họ và tên không được để trống") @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự") @Pattern(regexp = "^[a-zA-Z ]+$", message = "Họ và tên chỉ được chứa chữ cái và khoảng trắng") String fullName, @NotNull(message = "Ngày sinh không được để trống") @Past(message = "Ngày sinh phải là một ngày trong quá khứ") LocalDate birthDate, @NotNull(message = "Giới tính không được để trống") SexEnum sex) {
        super(phone, password, confirmPassword, fullName, birthDate, sex);
    }
}
