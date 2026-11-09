package iuh.fit.integration.services;


import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.integration.dtos.request.SocialRescueCandidateDto;
import iuh.fit.integration.entity.SocialRawFeed;
import iuh.fit.integration.enums.FeedStatus;
import iuh.fit.integration.enums.SourcePlatform;
import iuh.fit.integration.repository.SocialRawFeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;

import jakarta.validation.Validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SocialRescueCandidateService {

    private final SocialRawFeedRepository repository;
    private final Validator validator;

    public void ingest(List<SocialRescueCandidateDto> candidates) {
        if (candidates == null) {
            throw new IllegalArgumentException("Payload phải là một mảng JSON");
        }
        // Validate toàn bộ mảng trước khi bắt đầu ghi MongoDB.
        // Kafka listener không tự validate từng DTO chỉ vì có annotation.
        for (int index = 0; index < candidates.size(); index++) {
            SocialRescueCandidateDto candidate = candidates.get(index);

            if (candidate == null) {
                throw new BusinessException(ErrorCode.FORBIDDEN,
                        "Phần tử tại index " + index + " không được null"
                );
            }

            var violations = validator.validate(candidate);

            if (!violations.isEmpty()) {
                // Chỉ ghi tên field và thông báo, không đưa nội dung
                // bài đăng hoặc số điện thoại vào thông báo lỗi.
                String details = violations.stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .sorted()
                        .collect(Collectors.joining("; "));

                throw new BusinessException(
                        ErrorCode.BAD_GATEWAY,
                        "Dữ liệu không hợp lệ tại index " + index + ": " + details
                );
            }
        }

        for (SocialRescueCandidateDto candidate : candidates) {
            SocialRawFeed feed = toEntity(candidate);

            try {
                repository.insert(feed);

            } catch (DuplicateKeyException exception) {

                boolean alreadyExists =
                        repository.existsById(candidate.externalId());

                if (!alreadyExists) {
                    throw exception;
                }
            }

        }
    }


    private SocialRawFeed toEntity(SocialRescueCandidateDto dto) {
        // HashMap cho phép null: crawler có thể chưa tìm được
        // số điện thoại, địa chỉ hoặc tọa độ.
        Map<String, Object> extractedData = new HashMap<>();

        extractedData.put("externalId", dto.externalId());
        extractedData.put("source", dto.source());
        extractedData.put("groupId", dto.groupId());
        extractedData.put("groupUrl", dto.groupUrl());
        extractedData.put("postId", dto.postId());
        extractedData.put("content", dto.content());
        extractedData.put("reporterPhone", dto.reporterPhone());
        extractedData.put("address", dto.address());
        extractedData.put("incidentType", dto.incidentType());

        // Chuẩn hóa tên field lưu trong extractedData thành camelCase.
        extractedData.put("emergencyLevel", dto.emergencyLevel().name());

        extractedData.put("confidence", dto.confidence());
        extractedData.put("isEmergency", dto.isEmergency());
        extractedData.put("latitude", dto.latitude());
        extractedData.put("longitude", dto.longitude());

        // Giữ timestamp thu thập theo chuỗi ISO-8601 UTC.
        // createdAt của entity là thời điểm server tạo document.
        extractedData.put("collectedAt", dto.collectedAt().toString());

        return SocialRawFeed.builder()
                // externalId phải ổn định qua các lần crawl cùng một bài.
                .id(dto.externalId())

                // Enum hiện tại chỉ có FACEBOOK, chưa có FACEBOOK_GROUP.
                // Giá trị nguồn chi tiết vẫn được giữ trong extractedData.source.
                .sourcePlatform(SourcePlatform.FACEBOOK)
                .postUrl(dto.postUrl())
                .rawContent(dto.rawContent())
                .extractedData(extractedData)

                // Server tự đặt trạng thái, không lấy từ producer.
                .status(FeedStatus.PENDING)
                .build();
    }
}
