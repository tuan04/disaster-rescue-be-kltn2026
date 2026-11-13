package iuh.fit.integration.services;


import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.response.PageResponse;
import iuh.fit.integration.dtos.request.SocialRawFeedFilterRequest;
import iuh.fit.integration.dtos.request.UpdateSocialFeedRequest;
import iuh.fit.integration.entity.SocialRawFeed;
import iuh.fit.integration.enums.FeedStatus;
import iuh.fit.integration.repository.SocialFeedStateRepository;
import iuh.fit.integration.repository.SocialRawFeedQueryRepository;
import iuh.fit.integration.repository.SocialRawFeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SocialRawFeedService {
    private final SocialRawFeedQueryRepository queryRepository;
    private final SocialRawFeedRepository feedRepository;
    private final SocialFeedStateRepository stateRepository;

    public PageResponse<SocialRawFeed> searchFeeds(
            SocialRawFeedFilterRequest request,
            Pageable pageable
    ) {

        System.out.println("request: " + request);

        Sort sort = Sort.by(
                Sort.Order.desc("receivedAt"),
                Sort.Order.desc("id")
        );
        Pageable pageableNew = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );
        var pageData = queryRepository.search(request, pageableNew);
        return PageResponse.from(pageData);
    }



    public SocialRawFeed updateFeed(
            String feedId,
            UpdateSocialFeedRequest request
    ) {
        SocialRawFeed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy feed"
                ));

        if (feed.getStatus() != FeedStatus.PENDING) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Chỉ được cập nhật feed đang chờ duyệt"
            );
        }

        boolean hasLatitude = request.latitude() != null;
        boolean hasLongitude = request.longitude() != null;

        if (hasLatitude != hasLongitude) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Phải cập nhật latitude và longitude cùng nhau"
            );
        }

        if (hasLatitude && (
                !Double.isFinite(request.latitude())
                        || !Double.isFinite(request.longitude())
        )) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Tọa độ phải là số hữu hạn"
            );
        }

        Map<String, Object> changes = new HashMap<>();

        if (request.content() != null) {
            changes.put("content", request.content().strip());
        }
        if (request.reporterPhone() != null) {
            changes.put("reporterPhone", request.reporterPhone());
        }
        if (request.address() != null) {
            changes.put("address", request.address().strip());
        }
        if (hasLatitude) {
            changes.put("latitude", request.latitude());
            changes.put("longitude", request.longitude());
        }
        if (request.emergencyLevel() != null) {
            changes.put("emergencyLevel", request.emergencyLevel().name());
        }
        if (request.incidentType() != null) {
            changes.put("incidentType", request.incidentType().strip());
        }
        if (request.isEmergency() != null) {
            changes.put("isEmergency", request.isEmergency());
        }

        if (changes.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Cần cung cấp ít nhất một trường để cập nhật"
            );
        }

        SocialRawFeed updated =
                stateRepository.updateExtractedDataIfUnchanged(feed, changes);

        if (updated == null) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Feed đã thay đổi trong lúc cập nhật. Vui lòng tải lại"
            );
        }

        return updated;
    }
}
