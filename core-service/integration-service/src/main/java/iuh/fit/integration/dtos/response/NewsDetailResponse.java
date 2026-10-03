package iuh.fit.integration.dtos.response;

import iuh.fit.integration.entity.DisasterNews.NewsContentBlock;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record NewsDetailResponse(
        String id,
        String title,
        String summary,
        List<NewsContentBlock> content,
        String thumbnailUrl,
        String sourceUrl,
        String author,
        Instant publishedAt
) {
}
