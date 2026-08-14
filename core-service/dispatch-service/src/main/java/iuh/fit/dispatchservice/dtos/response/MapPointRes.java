package iuh.fit.dispatchservice.dtos.response;

import iuh.fit.dispatchservice.enums.PointType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class MapPointRes {
    private UUID id;
    private PointType pointType;
    private double latitude;
    private double longitude;
    private String priority;
    private String subType;
    private String status;
}
