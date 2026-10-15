package iuh.fit.integration.controller;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.common.response.PageResponse;
import iuh.fit.integration.dtos.request.SocialRawFeedFilterRequest;
import iuh.fit.integration.entity.SocialRawFeed;
import iuh.fit.integration.services.SocialManualApprovalService;
import iuh.fit.integration.services.SocialRawFeedService;
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
            @PathVariable("feedId") String feedId,
            @RequestHeader(value = "X-User-Role", required = true) String userRole
    ) {
        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            return ResponseEntity.status(403).body(
                    ApiResponse.<SocialRawFeed>builder()
                            .success(false)
                            .message("Bạn không có quyền phê duyệt feed")
                            .build()
            );
        }

        SocialRawFeed approvedFeed = socialManualApprovalService.approve(feedId);
        return ResponseEntity.ok(
                ApiResponse.success(approvedFeed, "Feed đã được phê duyệt thành công")
        );
    }
}
