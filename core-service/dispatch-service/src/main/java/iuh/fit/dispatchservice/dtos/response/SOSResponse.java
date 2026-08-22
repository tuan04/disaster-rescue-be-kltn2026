package iuh.fit.dispatchservice.dtos.response;

import iuh.fit.dispatchservice.enums.EmergencyLevel;
import iuh.fit.dispatchservice.enums.RequestSource;
import iuh.fit.dispatchservice.enums.RequestStatus;
import lombok.Builder;

import java.util.UUID;

@Builder
public record SOSResponse(
        UUID id,
        String reporterPhone,
        EmergencyLevel emergencyLevel,
        String content,
        RequestStatus status,
        RequestSource source,
        double latitude,
        double longitude
) {
}
