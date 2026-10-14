package iuh.fit.integration.repository;


import iuh.fit.common.social.SocialEvents;
import iuh.fit.integration.entity.SocialRawFeed;
import iuh.fit.integration.enums.FeedStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

// Spring quản lý repository này.
@Repository
@RequiredArgsConstructor
public class SocialFeedStateRepository {
    private final MongoTemplate mongoTemplate;

    // Tạo điều kiện dựa trên bản feed mà service đã đọc và kiểm tra.
    private Query unchangedPendingFeed(SocialRawFeed snapshot) {

        Criteria criteria = Criteria.where("id").is(snapshot.getId())

                // Chỉ xử lý feed còn chờ duyệt.
                .and("status").is(FeedStatus.PENDING)

                // Không ghi đè một sự kiện phê duyệt đã tồn tại.
                // is(null) khớp cả field null và field chưa có.
                .and("approvalEventId").is(null)
                .and("approvalPayload").is(null)
                .and("publishState").is(null)

                // Nguồn dữ liệu phải giống lúc xét.
                .and("sourcePlatform").is(snapshot.getSourcePlatform())

                // Đường dẫn bài phải giống lúc xét.
                .and("postUrl").is(snapshot.getPostUrl())

                // Mốc nhận feed không được thay đổi sau khi đọc.
                .and("receivedAt").is(snapshot.getReceivedAt())


                .and("autoNextCheckAt").is(snapshot.getAutoNextCheckAt());

        var extractedData = snapshot.getExtractedData();

        // Nếu lúc xét chưa có dữ liệu trích xuất...
        if (extractedData == null) {

            // ...thì chỉ cập nhật nếu hiện tại vẫn chưa có.
            criteria.and("extractedData").is(null);

        } else {

            // Đây là các field inspectFeed() dùng để quyết định
            // và tạo dữ liệu gửi dispatch.
            for (String field : List.of(
                    "isEmergency",
                    "confidence",
                    "content",
                    "reporterPhone",
                    "latitude",
                    "longitude",
                    "address",
                    "emergencyLevel",
                    "incidentType"
            )) {

                // So sánh từng field với giá trị tại thời điểm đọc.
                // Không so sánh toàn bộ Map để tránh phụ thuộc thứ tự key.
                criteria.and("extractedData." + field)
                        .is(extractedData.get(field));
            }
        }

        // Trả về query hoàn chỉnh cho thao tác cập nhật.
        return Query.query(criteria);
    }


    public boolean postponeIfUnchanged(
            SocialRawFeed snapshot,
            String reason,
            Instant nextCheckAt
    ) {

        Update update = new Update()
                .set("approvalReason", reason)
                .set("autoNextCheckAt", nextCheckAt);
        var result = mongoTemplate.updateFirst(
                unchangedPendingFeed(snapshot),
                update,
                SocialRawFeed.class
        );

        // ID là duy nhất nên tối đa một document được sửa.
        return result.getModifiedCount() == 1;
    }


    public boolean approveIfUnchanged(
            SocialRawFeed snapshot,
            SocialEvents.Approved event,
            String payload
    ) {

        Update update = new Update()
                .set("status", FeedStatus.APPROVED)
                .set("approvalMode", event.approvalMode())
                .set("approvedAt", event.approvedAt())
                .set("dispatchStatus", "QUEUED")
                .set("approvalEventId", event.eventId().toString())
                // Lưu nguyên JSON sẽ được worker gửi Kafka sau này.
                .set("approvalPayload", payload)
                .set("publishState", "PENDING")
                .set("publishNextAt", event.approvedAt())
                // Xóa lý do chưa đạt từ các lần xét trước.
                .unset("approvalReason")
                .unset("lastPublishError");

        var result = mongoTemplate.updateFirst(
                unchangedPendingFeed(snapshot),
                update,
                SocialRawFeed.class
        );

        return result.getModifiedCount() == 1;
    }


    public List<SocialRawFeed> findDueOutbox(
            Instant now,
            int batchSize
    ) {

        // Chỉ chọn feed đã duyệt và còn sự kiện đang chờ gửi.
        Query query = Query.query(
                Criteria.where("status").is(FeedStatus.APPROVED)
                        .and("publishState").is("PENDING")
                        .and("publishNextAt").lte(now)
        );

        // ID giúp thứ tự ổn định khi nhiều bản ghi có cùng thời gian.
        query.with(
                Sort.by(
                        Sort.Order.asc("publishNextAt"),
                        Sort.Order.asc("id")
                )
        );

        query.limit(batchSize);

        return mongoTemplate.find(query, SocialRawFeed.class);
    }

    private Query pendingOutbox(SocialRawFeed snapshot) {
        return Query.query(
                Criteria.where("id").is(snapshot.getId())
                        .and("approvalEventId").is(snapshot.getApprovalEventId())
                        .and("publishState").is("PENDING")
        );
    }


    // gọi sau khi bộ gửi đã nhận xác nhận thành công từ Kafka.
    public boolean markPublished(
            SocialRawFeed snapshot,
            Instant publishedAt
    ) {
        Update update = new Update()
                .set("publishState", "SENT")
                .set("publishedAt", publishedAt)

                .unset("publishNextAt")
                .unset("lastPublishError");

        var result = mongoTemplate.updateFirst(
                pendingOutbox(snapshot),
                update,
                SocialRawFeed.class
        );

        return result.getModifiedCount() == 1;
    }


    // Ghi nhận lần gửi chưa thành công và hẹn thời điểm thử lại.
    public boolean deferPublish(
            SocialRawFeed snapshot,
            String errorCode,
            Instant nextAttemptAt
    ) {

        Query query = pendingOutbox(snapshot);

        // Chỉ dời lịch nếu chưa có worker khác thay đổi lịch kể từ lúc đọc.
        // Tránh một worker xử lý chậm ghi đè lịch thử lại mới hơn.
        query.addCriteria(
                Criteria.where("publishNextAt")
                        .is(snapshot.getPublishNextAt())
        );

        Update update = new Update()
                .set("lastPublishError", errorCode)
                .set("publishNextAt", nextAttemptAt);

        // Nếu sự kiện đã SENT thì điều kiện không còn khớp và không cập nhật.
        var result = mongoTemplate.updateFirst(
                query,
                update,
                SocialRawFeed.class
        );

        // Cho biết thao tác có thực sự thay đổi document hay không.
        return result.getModifiedCount() == 1;
    }


    // Ghi nhận kết quả dispatch đã tạo yêu cầu cứu hộ.
    public boolean markDispatchCreated(SocialEvents.Created event) {
        String approvalEventId = event.approvalEventId().toString();
        String rescueRequestId = event.rescue().id().toString();

        // Trường hợp nhận kết quả lần đầu:
        // feed đang chờ dispatch và chưa có ID yêu cầu cứu hộ.
        Criteria waitingForResult = Criteria.where("dispatchStatus")
                .is("QUEUED")
                .and("rescueRequestId")
                .is(null);

        // Trường hợp Kafka giao lại cùng kết quả:
        // feed đã CREATED và ID yêu cầu đang lưu trùng với kết quả nhận được.
        Criteria sameResultAlreadyStored = Criteria.where("dispatchStatus")
                .is("CREATED")
                .and("rescueRequestId")
                .is(rescueRequestId);

        Criteria criteria = Criteria.where("id")
                .is(event.externalId())

                .and("status")
                .is(FeedStatus.APPROVED)

                .and("approvalEventId")
                .is(approvalEventId)

                .orOperator(
                        waitingForResult,
                        sameResultAlreadyStored
                );

        // Hai field kết quả được cập nhật cùng một thao tác.
        Update update = new Update()
                .set("dispatchStatus", "CREATED")
                .set("rescueRequestId", rescueRequestId);

        // Chỉ cập nhật document có sẵn, không tự tạo feed mới.
        // Spring ánh xạ tên field Java sang tên MongoDB theo entity.
        var result = mongoTemplate.updateFirst(
                Query.query(criteria),
                update,
                SocialRawFeed.class
        );

        return result.getMatchedCount() == 1;
    }


}
