package iuh.fit.integration.dtos.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UpdateSocialApprovalPolicyRequest(
        @NotNull(message = "enabled không được để trống")
        Boolean enabled,

        @NotNull(message = "minConfidence không được để trống")
        @DecimalMin(
                value = "0.0",
                message = "minConfidence phải lớn hơn hoặc bằng 0"
        )
        @DecimalMax(
                value = "1.0",
                message = "minConfidence phải nhỏ hơn hoặc bằng 1"
        )
        Double minConfidence
) {

}
