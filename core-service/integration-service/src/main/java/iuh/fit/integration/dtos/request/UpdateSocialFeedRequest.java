package iuh.fit.integration.dtos.request;

import iuh.fit.common.social.SocialEvents.EmergencyLevel;
import jakarta.validation.constraints.*;

public record UpdateSocialFeedRequest(
        @Size(max = 1000)
        @Pattern(
                regexp = "(?s).*\\S.*",
                message = "Nội dung không được chỉ chứa khoảng trắng"
        )
        String content,

        @Pattern(
                regexp = "^0[35789][0-9]{8}$",
                message = "Số điện thoại không hợp lệ"
        )
        String reporterPhone,

        @Size(max = 1000)
        String address,

        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        Double latitude,

        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        Double longitude,

        EmergencyLevel emergencyLevel,

        @Size(max = 100)
        String incidentType,

        Boolean isEmergency
) {
}