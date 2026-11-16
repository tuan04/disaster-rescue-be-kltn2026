package iuh.fit.userservice.dto.request;

import iuh.fit.userservice.enums.SexEnum;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateMyProfileRequest {

    @Size(max = 100)
    @Pattern(
            regexp = "(?s).*\\S.*",
            message = "Họ tên không được để trống"
    )
    private String fullName;

    private SexEnum sex;

    private MultipartFile avatar;

    @Past(message = "Ngày sinh phải trước ngày hiện tại")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;
}