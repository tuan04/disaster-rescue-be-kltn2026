package iuh.fit.integration.entity;

import iuh.fit.integration.enums.FeedStatus;
import iuh.fit.integration.enums.SourcePlatform;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "social_raw_feed")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialRawFeed {
    @Id
    private String id;

    @Field("source_platform")
    private SourcePlatform sourcePlatform;

    @Field("post_url")
    private String postUrl;

    @Field("raw_content")
    private String rawContent;

    @Field("extracted_data")
    private Map<String, Object> extractedData;

    @Field("is_valid_sos")
    private Boolean isValidSos;

    private FeedStatus status;

    @Field("created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
