package iuh.fit.intergationaiservice.entity;

import iuh.fit.intergationaiservice.enums.FeedStatus;
import iuh.fit.intergationaiservice.enums.SourcePlatform;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;


@Entity
@Table(name = "social_raw_feed")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialRawFeed {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_platform", length = 20)
    private SourcePlatform sourcePlatform;

    @Column(name = "post_url", length = 500)
    private String postUrl;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    // XỬ LÝ JSONB TUYỆT ĐỈNH CỦA HIBERNATE 6
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted_data", columnDefinition = "jsonb")
    private Map<String, Object> extractedData;

    @Column(name = "is_valid_sos")
    private Boolean isValidSos;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FeedStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
