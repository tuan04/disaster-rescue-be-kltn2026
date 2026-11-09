package iuh.fit.dispatchservice.dtos.request;


import iuh.fit.dispatchservice.enums.EmergencyLevel;
import iuh.fit.dispatchservice.enums.RequestSource;
import iuh.fit.dispatchservice.enums.RequestStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RescueRequestFilterRequest {

    private EmergencyLevel emergencyLevel;

    private RequestStatus status;

    private RequestSource source;

    private UUID locationId;

    @Size(max = 200, message = "keyword không được vượt quá 200 ký tự")
    private String keyword;
}
