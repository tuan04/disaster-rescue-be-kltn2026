package iuh.fit.integration.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.response.PageResponse;
import iuh.fit.integration.dtos.request.NewsIngestDto;
import iuh.fit.integration.dtos.response.NewsDetailResponse;
import iuh.fit.integration.dtos.response.NewsIngestResponse;
import iuh.fit.integration.dtos.response.NewsSummaryResponse;
import iuh.fit.integration.entity.DisasterNews;
import iuh.fit.integration.helper.DateTimeHelper;
import iuh.fit.integration.helper.NewsMapper;
import iuh.fit.integration.repository.DisasterNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsService {

    private final DisasterNewsRepository disasterNewsRepository;
    private final NewsMapper newsMapper;

    public NewsIngestResponse ingestNews(List<NewsIngestDto> dtos) {
        int total = dtos.size();
        int inserted = 0;
        int skipped = 0;
        List<DisasterNews> newsToSave = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        for (NewsIngestDto dto : dtos) {
            if (dto.getSourceUrl() == null || dto.getSourceUrl().trim().isEmpty()) {
                skipped++;
                continue;
            }

            String sourceUrl = dto.getSourceUrl().trim();

            // Chặn trùng lặp trong cùng 1 batch gửi lên HOẶC đã có trong DB
            if (!seenUrls.add(sourceUrl) || disasterNewsRepository.existsBySourceUrl(sourceUrl)) {
                skipped++;
                continue;
            }

            Instant publishedAt = DateTimeHelper.parseToInstant(dto.getPublishedAt());
            if (publishedAt == null) {
                skipped++;
                continue;
            }

            DisasterNews news = newsMapper.toEntity(dto, sourceUrl, publishedAt);
            newsToSave.add(news);
            inserted++;
        }

        if (!newsToSave.isEmpty()) {
            disasterNewsRepository.saveAll(newsToSave);
            log.info("Đã ingest thành công {} tin tức mới từ n8n (bỏ qua {} tin trùng/lỗi)", inserted, skipped);
        }

        return NewsIngestResponse.builder()
                .total(total)
                .inserted(inserted)
                .skipped(skipped)
                .build();
    }

    public PageResponse<NewsSummaryResponse> getNews(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));

        Page<DisasterNews> newsPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            newsPage = disasterNewsRepository.findByTitleContainingIgnoreCaseOrderByPublishedAtDesc(keyword.trim(), pageable);
        } else {
            newsPage = disasterNewsRepository.findAllByOrderByPublishedAtDesc(pageable);
        }

        Page<NewsSummaryResponse> summaryPage = newsPage.map(newsMapper::toSummaryResponse);
        return PageResponse.from(summaryPage);
    }

    public NewsDetailResponse getNewsById(String id) {
        DisasterNews news = disasterNewsRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy bài viết tin tức"));

        return newsMapper.toDetailResponse(news);
    }

    public List<NewsSummaryResponse> getLatestNews(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 10));
        Pageable pageable = PageRequest.of(0, safeLimit);

        Page<DisasterNews> newsPage = disasterNewsRepository.findAllByOrderByPublishedAtDesc(pageable);
        return newsPage.getContent().stream()
                .map(newsMapper::toSummaryResponse)
                .toList();
    }
}

