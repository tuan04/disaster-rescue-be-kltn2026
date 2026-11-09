package iuh.fit.integration.dtos.request;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SocialRescueCandidateDto(

        @NotBlank
        String externalId,

        @NotBlank
        String source,

        @NotBlank
        String groupId,

        @NotBlank
        String groupUrl,

        @NotBlank
        String postId,

        @NotBlank
        String postUrl,

        @NotBlank
        String rawContent,

        // Các thông tin AI chưa trích xuất được có thể là null.
        String content,

        String reporterPhone,

        String address,

        String incidentType,

        // Nhận đúng tên EmergencyLevel từ producer.
        // Đồng thời hỗ trợ emergencyLevel nếu sau này chuẩn hóa camelCase.
        @NotNull
        @JsonProperty("EmergencyLevel")
        @JsonAlias("emergencyLevel")
        EmergencyLevel emergencyLevel,

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        Double confidence,

        @NotNull
        @JsonProperty("isEmergency")
        Boolean isEmergency,

        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        Double latitude,

        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        Double longitude,

        // Instant đọc timestamp UTC có hậu tố Z.
        @NotNull
        Instant collectedAt
) {

    public enum EmergencyLevel {
        LOW,
        MEDIUM,
        HIGH
    }
}