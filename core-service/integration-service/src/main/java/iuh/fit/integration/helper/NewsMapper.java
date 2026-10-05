package iuh.fit.integration.helper;

import iuh.fit.integration.dtos.request.NewsIngestDto;
import iuh.fit.integration.dtos.response.NewsDetailResponse;
import iuh.fit.integration.dtos.response.NewsSummaryResponse;
import iuh.fit.integration.entity.DisasterNews;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;

@Mapper(componentModel = "spring")
public interface NewsMapper {

    NewsSummaryResponse toSummaryResponse(DisasterNews news);

    NewsDetailResponse toDetailResponse(DisasterNews news);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "sourceUrl", source = "sourceUrl")
    @Mapping(target = "publishedAt", source = "publishedAt")
    DisasterNews toEntity(NewsIngestDto dto, String sourceUrl, Instant publishedAt);
}
