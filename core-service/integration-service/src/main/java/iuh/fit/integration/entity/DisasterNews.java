package iuh.fit.integration.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Document(collection = "disaster_news")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisasterNews {

    @Id
    private String id;

    private String title;

    private String summary;

    private String content;

    @Field("thumbnail_url")
    private String thumbnailUrl;

    @Indexed(unique = true)
    @Field("source_url")
    private String sourceUrl;

    private String author;

    @Indexed
    @Field("published_at")
    private Instant publishedAt;

    private List<String> images;

    @Field("created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
