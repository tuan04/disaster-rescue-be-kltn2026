package iuh.fit.integration.entity;

import iuh.fit.integration.enums.FeedStatus;
import iuh.fit.integration.enums.SourcePlatform;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;




// Khai báo hai index phục vụ hai kiểu truy vấn định kỳ.
@CompoundIndexes({

        // Index dành cho bước xét feed đang PENDING.
        @CompoundIndex(
                // Tên index để dễ kiểm tra trong MongoDB Compass.
                name = "idx_social_auto_approval",

                // status: lọc trạng thái.
                // auto_next_check_at và _id: hỗ trợ thứ tự xử lý.
                // received_at: điều kiện thời gian áp dụng policy.
                def = """
                        {
                          "status": 1,
                          "auto_next_check_at": 1,
                          "_id": 1,
                          "received_at": 1
                        }
                        """
        ),

        // Index dành cho bước đọc outbox chờ gửi.
        @CompoundIndex(
                // Tên index của luồng phát sự kiện.
                name = "idx_social_outbox_due",

                // Hai field đầu là điều kiện bằng.
                // Hai field sau khớp thứ tự sắp xếp của findDueOutbox().
                def = """
                        {
                          "status": 1,
                          "publish_state": 1,
                          "publish_next_at": 1,
                          "_id": 1
                        }
                        """
        )
})


@Document(collection = "social_raw_feed")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialRawFeed {
    @Id
    private String id;

    @Field("source_platform")
    private SourcePlatform sourcePlatform;

    @Field("post_url")
    private String postUrl;

    @Field("raw_content")
    private String rawContent;

    @Field("extracted_data")
    private Map<String, Object> extractedData;

    private FeedStatus status;


    @Field("received_at")
    @Builder.Default
    // Thời điểm integration tạo feed mới từ dữ liệu nhận được.
    private Instant receivedAt = Instant.now();



    @Field("auto_next_check_at")
    @Builder.Default
    // Thời điểm sớm nhất worker được xét lại feed.
    // Khi chưa đủ điều kiện, service sẽ dời thời điểm này về sau.
    private Instant autoNextCheckAt = Instant.now();


    @Field("approval_reason")
    // Lưu lý do feed chưa được tự duyệt.
    private String approvalReason;


    @Field("approval_mode")
    // Ghi nhận cách phê duyệt: AUTO hoặc MANUAL.
    // Feed đang PENDING thì trường này chưa cần có giá trị.
    private String approvalMode;


    @Field("approved_at")
    // Thời điểm feed được chuyển sang APPROVED.
    private Instant approvedAt;


    @Field("dispatch_status")
    @Builder.Default
    // Feed mới chưa yêu cầu dispatch tạo dữ liệu. NOT_REQUESTED
    // Sau khi duyệt: QUEUED.
    // Sau khi nhận kết quả từ dispatch: CREATED.
    private String dispatchStatus = "NOT_REQUESTED";


    @Field("rescue_request_id")
    // ID yêu cầu cứu hộ được dispatch tạo.
    // ID gốc bên PostgreSQL là UUID; ở MongoDB ta lưu biểu diễn chuỗi của UUID.
    // Chỉ gán khi nhận kết quả thành công từ dispatch.
    private String rescueRequestId;


// ==================== OUTBOX NHÚNG TRONG FEED ====================

    @Field("approval_event_id")
    // ID của sự kiện SocialEvents.Approved.
    private String approvalEventId;

    @Field("approval_payload")
    // Chuỗi JSON của SocialEvents.Approved tại thời điểm phê duyệt.
    private String approvalPayload;


    @Field("publish_state")
    // null: chưa có sự kiện phê duyệt cần gửi.
    // "PENDING": đã có sự kiện phê duyệt cần gửi.
    // "SENT": đã gửi sự kiện phê duyệt thành công.
    private String publishState;


    // Đặt tên field MongoDB là publish_next_at.
    @Field("publish_next_at")
    // Thời điểm sớm nhất worker được thử gửi sự kiện.
    private Instant publishNextAt;


    @Field("published_at")
    // Thời điểm server ghi nhận Kafka đã xác nhận nhận sự kiện.
    private Instant publishedAt;


    @Field("last_publish_error")
    // Lưu thông tin lỗi của lần gửi gần nhất để theo dõi.
    private String lastPublishError;



    @Field("created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
