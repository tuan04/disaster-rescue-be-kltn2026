package iuh.fit.integration.dtos.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record NewsSummaryResponse(
        String id,
        String title,
        String summary,
        String thumbnailUrl,
        String author,
        Instant publishedAt
) {
}
