package iuh.fit.integration.services;



import iuh.fit.common.social.SocialEvents;
import iuh.fit.integration.dtos.request.FeedInspection;
import iuh.fit.integration.entity.SocialRawFeed;
import iuh.fit.integration.enums.SourcePlatform;
import iuh.fit.integration.repository.SocialFeedStateRepository;
import iuh.fit.integration.repository.SocialRawFeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import jakarta.validation.Validator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class SocialApprovalFlow {
    private static final int BATCH_SIZE = 50;
    // Feed chưa đạt sẽ được xét lại sớm nhất sau 60 giây.
    private static final long RECHECK_DELAY_SECONDS = 60;
    private final SocialApprovalPolicyService policyService;

    private final SocialRawFeedRepository feedRepository;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final SocialFeedStateRepository stateRepository;


    public List<FeedInspection> inspectPendingFeeds() {

        var policy = policyService.getPolicy();


        if (!Boolean.TRUE.equals(policy.enabled())) {
            return List.of();
        }

        Instant now = Instant.now();


         Pageable pageRequest = PageRequest.of(
                0,
                BATCH_SIZE,
                Sort.by(
                        Sort.Order.asc("autoNextCheckAt"),
                        Sort.Order.asc("id")
                )
        );

        List<SocialRawFeed> feeds = feedRepository.findDuePendingFeeds(
                policy.enabledSince(),
                now,
                pageRequest
        );


        List<FeedInspection> results = new ArrayList<>(feeds.size());

        for (SocialRawFeed feed : feeds) {
            FeedInspection result = inspectFeed(
                    feed,
                    policy.minConfidence()
            );
            results.add(result);
        }

        return List.copyOf(results);
    }


    private FeedInspection inspectFeed(
            SocialRawFeed feed,
            double minConfidence
    ) {
        if (feed.getSourcePlatform() != SourcePlatform.FACEBOOK) {
            return notEligible(feed, "UNSUPPORTED_SOURCE");
        }

        var extractedData = feed.getExtractedData();

        if (extractedData == null) {
            return notEligible(feed, "MISSING_EXTRACTED_DATA");
        }

        if (!Boolean.TRUE.equals(extractedData.get("isEmergency"))) {
            return notEligible(feed, "NOT_MARKED_EMERGENCY");
        }

        // Map có kiểu Object nên cần kiểm tra kiểu trước khi đọc giá trị số.
        Object rawConfidence = extractedData.get("confidence");


        if (!(rawConfidence instanceof Number number)) {
            return notEligible(feed, "INVALID_CONFIDENCE");
        }


        double confidence = number.doubleValue();


        if (!Double.isFinite(confidence)
                || confidence < 0
                || confidence > 1) {
            return notEligible(feed, "INVALID_CONFIDENCE");
        }

        if (confidence < minConfidence) {
            return notEligible(feed, "CONFIDENCE_TOO_LOW");
        }

        // Sao chép dữ liệu để việc chuẩn bị DTO không sửa Map của feed.
        var values = new HashMap<>(extractedData);

        values.put("source", feed.getSourcePlatform().name());

        values.put("postUrl", feed.getPostUrl());

        SocialEvents.RescueData data;

        try {

            data = objectMapper.convertValue(
                    values,
                    SocialEvents.RescueData.class
            );

        } catch (JacksonException | IllegalArgumentException exception) {
            return notEligible(feed, "INVALID_RESCUE_DATA_FORMAT");
        }

        var violations = validator.validate(data);

        if (!violations.isEmpty()) {

            // Chỉ gom tên field lỗi để kết quả dễ đọc.
            String invalidFields = violations.stream()
                    .map(violation ->
                            violation.getPropertyPath().toString()
                    )

                    .distinct()
                    .sorted()
                    .collect(Collectors.joining(", "));

            return notEligible(
                    feed,
                    "INVALID_RESCUE_DATA: " + invalidFields
            );
        }

        if (!Double.isFinite(data.latitude())
                || !Double.isFinite(data.longitude())) {
            return notEligible(feed, "INVALID_COORDINATES");
        }

        return new FeedInspection(
                feed.getId(),
                true,
                null,
                data
        );
    }

    public int approvePendingFeeds() {

        var policy = policyService.getPolicy();

        if (!Boolean.TRUE.equals(policy.enabled())) {
            return 0;
        }

        Instant now = Instant.now();

        Pageable pageRequest = PageRequest.of(
                0,
                BATCH_SIZE,
                Sort.by(
                        Sort.Order.asc("autoNextCheckAt"),
                        Sort.Order.asc("id")
                )
        );


        List<SocialRawFeed> feeds = feedRepository.findDuePendingFeeds(
                policy.enabledSince(),
                now,
                pageRequest
        );

        int approvedCount = 0;

        // Xử lý từng feed trong danh sách.
        for (SocialRawFeed feed : feeds) {

            // Tái sử dụng toàn bộ logic kiểm tra của phần trước.
            FeedInspection inspection = inspectFeed(
                    feed,
                    policy.minConfidence()
            );

            // Nhánh 1: feed chưa đủ điều kiện.
            if (!inspection.eligible()) {

                Instant nextCheckAt = Instant.now()
                        .plusSeconds(RECHECK_DELAY_SECONDS);

                // Chỉ ghi lý do nếu feed vẫn giống lúc vừa xét.
                // Nếu trả false, feed đã thay đổi; để lượt sau đọc lại.
                stateRepository.postponeIfUnchanged(
                        feed,
                        inspection.reason(),
                        nextCheckAt
                );
                continue;
            }

            // Nhánh 2: feed đủ điều kiện.
            // Ghi nhận thời điểm tạo quyết định phê duyệt.
            Instant approvedAt = Instant.now();

            // Tạo sự kiện chứa dữ liệu vừa được kiểm tra.
            SocialEvents.Approved event = new SocialEvents.Approved(
                    UUID.randomUUID(),
                    feed.getId(),
                    approvedAt,
                    "AUTO",
                    inspection.data()
            );

            var violations = validator.validate(event);

            // Nếu sự kiện chưa hợp lệ thì không được ghi APPROVED.
            if (!violations.isEmpty()) {

                // Giữ feed chờ xử lý và ghi lý do.
                stateRepository.postponeIfUnchanged(
                        feed,
                        "INVALID_APPROVAL_EVENT",
                        Instant.now().plusSeconds(RECHECK_DELAY_SECONDS)
                );

                // Tiếp tục các feed khác.
                continue;
            }

            String payload = objectMapper.writeValueAsString(event);

            // Ghi quyết định và JSON sự kiện cùng lúc.
            // Repository kiểm tra lại snapshot tại database.
            boolean approved = stateRepository.approveIfUnchanged(
                    feed,
                    event,
                    payload
            );

            // Chỉ đếm khi database thực sự áp dụng cập nhật.
            if (approved) {
                approvedCount++;
            }

            // Nếu approved = false, không gửi sự kiện vừa tạo.
            // Sự kiện đó chỉ tồn tại tạm trong bộ nhớ và được bỏ qua.
            // Worker gửi Kafka sau này chỉ đọc payload đã lưu trong MongoDB.
        }

        // Trả số feed đã được duyệt, không phải số feed đã kiểm tra.
        return approvedCount;
    }



    private FeedInspection notEligible(
            SocialRawFeed feed,
            String reason
    ) {
        return new FeedInspection(
                feed.getId(),
                false,
                reason,
                null
        );
    }




}
