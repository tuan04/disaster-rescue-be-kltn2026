package iuh.fit.integration.controller;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.common.response.PageResponse;
import iuh.fit.integration.dtos.request.SocialRawFeedFilterRequest;
import iuh.fit.integration.dtos.request.UpdateSocialFeedRequest;
import iuh.fit.integration.entity.SocialRawFeed;
import iuh.fit.integration.services.SocialManualApprovalService;
import iuh.fit.integration.services.SocialRawFeedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/social-feeds")
@RequiredArgsConstructor
public class SocialRawFeedController {
    private final SocialRawFeedService socialRawFeedService;
    private final SocialManualApprovalService socialManualApprovalService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SocialRawFeed>>> searchFeeds(
            @ModelAttribute SocialRawFeedFilterRequest request,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        socialRawFeedService.searchFeeds(request, pageable)
                )
        );
    }

    @PostMapping("/{feedId}/approve")
    public ResponseEntity<ApiResponse<SocialRawFeed>> approveFeed(
            @PathVariable("feedId") String feedId
    ) {
        SocialRawFeed approvedFeed = socialManualApprovalService.approve(feedId);
        return ResponseEntity.ok(
                ApiResponse.success(approvedFeed, "Feed đã được phê duyệt thành công")
        );
    }



    @PostMapping("/{feedId}/reject")
    public ResponseEntity<ApiResponse<SocialRawFeed>> rejectFeed(
            @PathVariable("feedId") String feedId
    ) {
        SocialRawFeed approvedFeed = socialManualApprovalService.reject(feedId);
        return ResponseEntity.ok(
                ApiResponse.success(approvedFeed, "Feed đã được phê duyệt thành công")
        );
    }



    @PatchMapping("/{feedId}")
    public ResponseEntity<ApiResponse<SocialRawFeed>> updateFeed(
            @PathVariable("feedId") String feedId,
            @Valid @RequestBody UpdateSocialFeedRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        socialRawFeedService.updateFeed(feedId, request),
                        "Cập nhật feed thành công"
                )
        );
    }




}
