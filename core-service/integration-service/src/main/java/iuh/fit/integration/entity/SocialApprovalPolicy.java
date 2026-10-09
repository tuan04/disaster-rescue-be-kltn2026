
package iuh.fit.integration.entity;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;


@Document(collection = "social_approval_policy")
public record SocialApprovalPolicy(
        @Id
        @NotBlank(message = "ID cấu hình không được để trống")
        @Pattern(
                regexp = "social-auto",
                message = "ID cấu hình phải là social-auto"
        )
        String id,

        @NotNull(message = "Trạng thái bật/tắt không được để trống")
        Boolean enabled,

        @Field("min_confidence")
        @NotNull(message = "Ngưỡng confidence không được để trống")
        @DecimalMin(
                value = "0.0",
                message = "Ngưỡng confidence phải >= 0"
        )

        @DecimalMax(
                value = "1.0",
                message = "Ngưỡng confidence phải <= 1"
        )
        Double minConfidence,

        @Field("enabled_since")
        Instant enabledSince,



        @Version
        Long version

) {

    public static final String POLICY_ID = "social-auto";

    // Bean Validation sẽ gọi method này khi validate object policy.
    // @AssertTrue yêu cầu giá trị trả về phải là true.
    @AssertTrue(
            message = "Phải có thời điểm bắt đầu khi bật tự động phê duyệt"
    )
    public boolean isEnabledSinceValid() {
        return !Boolean.TRUE.equals(enabled) || enabledSince != null;
    }
}