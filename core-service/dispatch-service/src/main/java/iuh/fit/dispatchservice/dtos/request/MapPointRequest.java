package iuh.fit.dispatchservice.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MapPointRequest {
    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    @NotNull(message = "Vĩ độ (latitude) không được để trống")
    private Double latitude;


    @NotNull(message = "Kinh độ (longitude) không được để trống")
    private Double longitude;
}

