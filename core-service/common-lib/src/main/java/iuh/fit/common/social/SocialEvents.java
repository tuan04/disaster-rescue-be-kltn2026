package iuh.fit.common.social;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import iuh.fit.common.kafka.dto.SOSResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

public final class SocialEvents {
    private SocialEvents() {}

    public static final String APPROVED_TOPIC = "social-rescue-approved";
    public static final String CREATED_TOPIC = "social-rescue-created";
    public static final String NOTIFICATION_TOPIC = "social-rescue-notifications";

    public enum EmergencyLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RescueData(

            @NotBlank(message = "Nguồn dữ liệu không được để trống")
            @Pattern(
                    regexp = "FACEBOOK",
                    message = "Nguồn dữ liệu phải là FACEBOOK"
            )
            String source,

            @NotBlank(message = "Đường dẫn bài đăng không được để trống")
            String postUrl,

            @Pattern(
                    regexp = "^0[35789][0-9]{8}$",
                    message = "Số điện thoại không hợp lệ"
            )
            String reporterPhone,

            @NotBlank(message = "Nội dung cứu hộ không được để trống")
            @Size(
                    max = 1000,
                    message = "Nội dung không được vượt quá 1000 ký tự"
            )
            String content,

            @NotNull(message = "Vĩ độ không được để trống")
            @DecimalMin(value = "-90.0", message = "Vĩ độ phải >= -90")
            @DecimalMax(value = "90.0", message = "Vĩ độ phải <= 90")
            Double latitude,

            @NotNull(message = "Kinh độ không được để trống")
            @DecimalMin(value = "-180.0", message = "Kinh độ phải >= -180")
            @DecimalMax(value = "180.0", message = "Kinh độ phải <= 180")
            Double longitude,

            String address,

            @NotNull(message = "Mức khẩn cấp không được để trống")
            EmergencyLevel emergencyLevel,

            String incidentType
    ) {
        // DTO chỉ chứa dữ liệu nên chưa cần thêm method.
    }


    public record Approved(
            @NotNull(message = "ID sự kiện không được để trống")
            UUID eventId,

            @NotBlank(message = "externalId không được để trống")
            String externalId,

            @NotNull(message = "Thời điểm phê duyệt không được để trống")
            Instant approvedAt,


            @NotBlank(message = "Cách phê duyệt không được để trống")
            @Pattern(
                    regexp = "AUTO|MANUAL",
                    message = "Cách phê duyệt phải là AUTO hoặc MANUAL"
            )
            String approvalMode,


            @NotNull(message = "Dữ liệu cứu hộ không được để trống")
            @Valid
            RescueData data
    ) {
        // Đây cũng là DTO, chưa thực hiện nghiệp vụ hoặc gọi database.
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Created(

            // ID mới của sự kiện "đã tạo yêu cầu cứu hộ".
            @NotNull(message = "ID sự kiện kết quả không được để trống")
            UUID eventId,

            // ID sự kiện Approved đã dẫn đến việc tạo yêu cầu này.
            // Integration dùng để đối chiếu với approvalEventId đang lưu.
            @NotNull(message = "ID sự kiện phê duyệt không được để trống")
            UUID approvalEventId,

            // ID gốc của feed bên integration.
            @NotBlank(message = "externalId không được để trống")
            String externalId,

            // Thông tin yêu cầu cứu hộ vừa tạo.
            // rescue.id() là UUID của RescueRequest bên dispatch.
            @NotNull(message = "Thông tin yêu cầu cứu hộ không được để trống")
            @Valid
            SOSResponse rescue
    ) {
        // Record chỉ chứa dữ liệu sự kiện, không thực hiện nghiệp vụ.
    }



}
