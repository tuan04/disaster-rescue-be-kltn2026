package iuh.fit.integration.dtos.request;

import iuh.fit.integration.entity.DisasterNews.NewsContentBlock;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsIngestDto {

    private String title;

    private String summary;

    private List<NewsContentBlock> content;

    private String thumbnailUrl;

    private String sourceUrl;

    private String author;

    private String publishedAt;
}
