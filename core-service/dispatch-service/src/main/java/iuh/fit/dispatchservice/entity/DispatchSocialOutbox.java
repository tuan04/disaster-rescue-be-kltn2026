package iuh.fit.dispatchservice.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(
        name = "dispatch_social_outbox",
        indexes = @Index(
                name = "idx_dispatch_social_outbox_due",
                columnList = "publish_state,publish_next_at,event_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchSocialOutbox {
    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "topic", nullable = false)
    private String topic;

    // Kafka key; với luồng này dùng externalId.
    @Column(name = "message_key", nullable = false, columnDefinition = "TEXT")
    private String messageKey;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    // PENDING: chờ gửi; SENT: Kafka đã xác nhận.
    @Column(name = "publish_state", nullable = false, length = 20)

    // Giữ giá trị mặc định khi khởi tạo bằng builder.
    @Builder.Default
    private String publishState = "PENDING";

    // Thời điểm sớm nhất được phép gửi hoặc thử lại.
    // Có thể được xóa sau khi chuyển SENT.
    @Column(name = "publish_next_at")
    @Builder.Default
    private Instant publishNextAt = Instant.now();

    // Thời điểm worker ghi nhận Kafka đã xác nhận.
    // Trước khi gửi thành công thì để null.
    @Column(name = "published_at")
    private Instant publishedAt;

    // Mã lỗi của lần gửi gần nhất, nếu có.
    @Column(name = "last_publish_error", columnDefinition = "TEXT")
    private String lastPublishError;

    // Thời điểm tạo bản ghi outbox.
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
