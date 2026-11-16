package iuh.fit.resourcemanagementservice.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateTeamMobileInventoryRequest(
        @NotNull(message = "campaignTeamId không được để trống")
        UUID campaignTeamId,

        @NotEmpty(message = "Danh sách vật phẩm bổ sung không được để trống")
        @Valid
        List<TeamItemImport> items
) {
    public record TeamItemImport(
            @NotNull(message = "itemId không được để trống")
            UUID itemId,

            @NotNull(message = "Số lượng không được để trống")
            @Min(value = 0, message = "Số lượng không được âm")
            Integer currentQuantity
    ) {}
}
