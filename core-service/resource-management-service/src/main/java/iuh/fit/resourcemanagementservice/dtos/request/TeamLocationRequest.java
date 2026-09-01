package iuh.fit.resourcemanagementservice.dtos.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record TeamLocationRequest(
        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        Double latitude,

        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        Double longitude,

        @DecimalMin("0.0")
        Double speed,

        @DecimalMin("0.0")
        @DecimalMax("360.0")
        Double heading
) {
}
