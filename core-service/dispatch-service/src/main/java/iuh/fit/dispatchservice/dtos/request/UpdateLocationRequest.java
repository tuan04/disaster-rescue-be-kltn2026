package iuh.fit.dispatchservice.dtos.request;

import iuh.fit.dispatchservice.enums.LocationStatus;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateLocationRequest {
    private UUID userId;

    private String name;

    @Min(value = 1, message = "Bán kính phải lớn hơn 0 mét")
    private Integer radiusMeters;

    private LocationStatus status;

    private Boolean isActive;
}
