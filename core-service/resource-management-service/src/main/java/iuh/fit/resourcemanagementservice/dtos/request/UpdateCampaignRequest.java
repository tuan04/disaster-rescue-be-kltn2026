package iuh.fit.resourcemanagementservice.dtos.request;

import iuh.fit.resourcemanagementservice.enums.CampaignStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCampaignRequest {

    @NotBlank(message = "Tên chiến dịch không được để trống")
    @Size(max = 255, message = "Tên chiến dịch không được vượt quá 255 ký tự")
    private String name;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "Trạng thái chiến dịch không được để trống")
    private CampaignStatus status;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    @Size(max = 255, message = "Tên tỉnh/thành phố không được vượt quá 255 ký tự")
    private String province;
}
