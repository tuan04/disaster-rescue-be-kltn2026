package iuh.fit.integration.dtos.response;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record NewsDetailResponse(
        String id,
        String title,
        String summary,
        String content,
        String thumbnailUrl,
        List<String> images,
        String sourceUrl,
        String author,
        Instant publishedAt
) {
}
