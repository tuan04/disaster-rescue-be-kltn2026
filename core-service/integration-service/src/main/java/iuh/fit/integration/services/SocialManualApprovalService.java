package iuh.fit.integration.services;


import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.social.SocialEvents;
import iuh.fit.integration.entity.SocialRawFeed;
import iuh.fit.integration.enums.FeedStatus;
import iuh.fit.integration.repository.SocialFeedStateRepository;
import iuh.fit.integration.repository.SocialRawFeedRepository;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SocialManualApprovalService {
    private final SocialRawFeedRepository feedRepository;
    private final SocialFeedStateRepository stateRepository;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public SocialRawFeed approve(String feedId) {

        if (feedId == null || feedId.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "ID feed không được để trống"
            );
        }

        SocialRawFeed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy feed"
                ));

        if (feed.getStatus() != FeedStatus.PENDING) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Chỉ được phê duyệt feed đang PENDING"
            );
        }

        SocialEvents.RescueData data = buildRescueData(feed);

        // Tạo sự kiện theo đúng contract hiện dispatch đang nhận.
        SocialEvents.Approved event = new SocialEvents.Approved(
                UUID.randomUUID(),
                feed.getId(),
                Instant.now(),
                "MANUAL",
                data
        );

        // Kiểm tra cả sự kiện và RescueData bên trong nhờ @Valid.
        // Ví dụ: content, tọa độ, emergencyLevel, số điện thoại...
        var violations = validator.validate(event);

        if (!violations.isEmpty()) {
            // Trả lý do cụ thể để admin biết dữ liệu nào cần sửa.
            String message = violations.stream()
                    .map(violation ->
                            violation.getPropertyPath()
                                    + ": "
                                    + violation.getMessage()
                    )
                    .sorted()
                    .collect(Collectors.joining("; "));

            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Feed chưa đủ dữ liệu hợp lệ để duyệt: " + message
            );
        }

        // Sau validation, latitude và longitude đã được kiểm tra khác null.
        // Kiểm tra thêm để loại NaN và Infinity.
        if (!Double.isFinite(data.latitude())
                || !Double.isFinite(data.longitude())) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Tọa độ phải là số hữu hạn"
            );
        }

        String payload = objectMapper.writeValueAsString(event);

        boolean approved = stateRepository.approveIfUnchanged(
                feed,
                event,
                payload
        );

        if (!approved) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Feed đã thay đổi hoặc đã được xử lý. "
                            + "Vui lòng tải lại trước khi phê duyệt"
            );
        }

        return feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Feed không còn tồn tại sau khi phê duyệt"
                ));
    }

    private SocialEvents.RescueData buildRescueData(SocialRawFeed feed) {
        // Không thể tạo yêu cầu cứu hộ nếu chưa có dữ liệu trích xuất.
        if (feed.getExtractedData() == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Feed chưa có dữ liệu trích xuất"
            );
        }

        if (feed.getSourcePlatform() == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Feed chưa có nguồn dữ liệu"
            );
        }
        var values = new HashMap<>(feed.getExtractedData());
        values.put("source", feed.getSourcePlatform().name());
        values.put("postUrl", feed.getPostUrl());

        try {
            return objectMapper.convertValue(
                    values,
                    SocialEvents.RescueData.class
            );

        } catch (JacksonException | IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Dữ liệu cứu hộ không đúng định dạng. "
                            + "Vui lòng kiểm tra tọa độ và emergencyLevel"
            );
        }
    }
}
