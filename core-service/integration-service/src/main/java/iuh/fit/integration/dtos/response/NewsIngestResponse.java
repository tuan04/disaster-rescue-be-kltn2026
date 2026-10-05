package iuh.fit.integration.dtos.response;

import lombok.Builder;

@Builder
public record NewsIngestResponse(
        int total,
        int inserted,
        int skipped
) {
}
