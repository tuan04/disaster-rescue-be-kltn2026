package iuh.fit.dispatchservice.dtos.response;

import iuh.fit.dispatchservice.enums.EmergencyLevel;
import iuh.fit.dispatchservice.enums.RequestSource;
import iuh.fit.dispatchservice.enums.RequestStatus;

import java.util.UUID;

public record RescueDetailResponse(
        UUID id,
        String reporterPhone,
        EmergencyLevel emergencyLevel,
        String content,
        RequestStatus status,
        RequestSource source
) implements MapPointDetailResInterface {
}
