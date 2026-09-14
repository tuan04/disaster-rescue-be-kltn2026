package iuh.fit.dispatchservice.dtos.request;

import iuh.fit.dispatchservice.enums.HazardStatus;
import iuh.fit.dispatchservice.enums.HazardType;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateHazardReportRequest {
    private UUID id;

    private HazardType hazardType;

    private String description;

    private HazardStatus status;

    private List<String> imageUrls;

    private List<MultipartFile> images;
}
