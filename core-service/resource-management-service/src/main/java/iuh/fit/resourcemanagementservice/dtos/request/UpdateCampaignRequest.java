package iuh.fit.resourcemanagementservice.dtos.request;

import iuh.fit.resourcemanagementservice.enums.CampaignStatus;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCampaignRequest {

    @Size(max = 255, message = "Tên chiến dịch không được vượt quá 255 ký tự")
    private String name;

    private LocalDate startDate;

    private LocalDate endDate;

    private CampaignStatus status;
}
