package iuh.fit.integration.controller;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.common.response.PageResponse;
import iuh.fit.integration.dtos.request.SocialRawFeedFilterRequest;
import iuh.fit.integration.entity.SocialRawFeed;
import iuh.fit.integration.services.SocialRawFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/social-feeds")
@RequiredArgsConstructor
public class SocialRawFeedController {
    private final SocialRawFeedService socialRawFeedService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SocialRawFeed>>> searchFeeds(
            SocialRawFeedFilterRequest request,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        socialRawFeedService.searchFeeds(request, pageable)
                )
        );
    }
}
