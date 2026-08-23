package iuh.fit.userservice.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class UpgradeRescuerRequest {
    @NotEmpty(message = "Id không được để trống")
    private UUID id;

    @NotBlank(message = "Số CCCD không được để trống")
    @Pattern(
            regexp = "^0(0[1-9]|[1-8][0-9]|9[0-6])[0-3][0-9]{8}$",
            message = "Số CCCD không hợp lệ hoặc sai định dạng quốc gia"
    )
    private String CCCD;
}
