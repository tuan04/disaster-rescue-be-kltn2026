package iuh.fit.integration.controller;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.response.ApiResponse;
import iuh.fit.common.response.PageResponse;
import iuh.fit.integration.dtos.request.NewsIngestDto;
import iuh.fit.integration.dtos.response.NewsDetailResponse;
import iuh.fit.integration.dtos.response.NewsIngestResponse;
import iuh.fit.integration.dtos.response.NewsSummaryResponse;
import iuh.fit.integration.services.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
@Slf4j
public class NewsController {

    private final NewsService newsService;

    @Value("${n8n.webhook.secret-key}")
    private String expectedSecretKey;

    @PostMapping("/ingest")
    public ResponseEntity<ApiResponse<NewsIngestResponse>> ingestNews(
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey,
            @RequestBody List<NewsIngestDto> dtos
    ) {
        if (apiKey == null || !apiKey.equals(expectedSecretKey)) {
            log.warn("Từ chối request ingest: Header X-API-KEY không hợp lệ hoặc bị thiếu");
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        NewsIngestResponse response = newsService.ingestNews(dtos);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NewsSummaryResponse>>> getNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        PageResponse<NewsSummaryResponse> response = newsService.getNews(page, size, keyword);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<List<NewsSummaryResponse>>> getLatestNews(
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<NewsSummaryResponse> response = newsService.getLatestNews(limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NewsDetailResponse>> getNewsById(@PathVariable String id) {
        NewsDetailResponse response = newsService.getNewsById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
