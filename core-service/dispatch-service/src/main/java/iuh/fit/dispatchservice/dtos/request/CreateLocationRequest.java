package iuh.fit.dispatchservice.dtos.request;

import iuh.fit.dispatchservice.dtos.response.GeoJsonPolygon;
import iuh.fit.dispatchservice.enums.LocationStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateLocationRequest {
    @NotBlank(message = "Tên khu vực không được để trống")
    private String name;

    private UUID userId;

    private GeoJsonPolygon boundary;

    @Min(value = 1, message = "Bán kính phải lớn hơn 0 mét")
    private Integer radiusMeters;

    @NotNull(message = "Trạng thái không được để trống")
    private LocationStatus status;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean isActive;
}
