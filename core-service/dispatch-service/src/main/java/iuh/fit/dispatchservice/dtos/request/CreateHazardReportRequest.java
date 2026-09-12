package iuh.fit.dispatchservice.dtos.request;

import iuh.fit.dispatchservice.enums.HazardType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateHazardReportRequest {
    @NotNull(message = "Loại hiểm họa không được để trống")
    private HazardType hazardType;

    private String description;

    @Valid
    private MapPointRequest mapPoint;

    private String address;
    private Double latitude;
    private Double longitude;

    private List<MultipartFile> images;

    public MapPointRequest resolveMapPoint() {
        if (mapPoint != null && (mapPoint.getLatitude() != null || mapPoint.getLongitude() != null || mapPoint.getAddress() != null)) {
            return mapPoint;
        }
        if (latitude != null || longitude != null || address != null) {
            return MapPointRequest.builder()
                    .address(address)
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();
        }
        return mapPoint;
    }
}

