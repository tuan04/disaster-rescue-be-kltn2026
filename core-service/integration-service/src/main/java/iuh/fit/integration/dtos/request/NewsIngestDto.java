package iuh.fit.integration.dtos.request;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    private String content;

    private String thumbnailUrl;

    private String sourceUrl;

    private String author;

    private String publishedAt;

    private List<String> images;
}
