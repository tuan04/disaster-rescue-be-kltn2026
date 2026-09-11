package iuh.fit.resourcemanagementservice.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTeamMobileInventoryRequest(
        @NotNull(message = "campaignTeamId không được để trống")
        UUID campaignTeamId,

        @NotNull(message = "itemId không được để trống")
        UUID itemId,

        @NotNull(message = "currentQuantity không được để trống")
        @Min(value = 0, message = "Số lượng không được âm")
        Integer currentQuantity
) {
}
