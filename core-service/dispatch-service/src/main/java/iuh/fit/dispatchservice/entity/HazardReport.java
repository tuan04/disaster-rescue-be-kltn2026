package iuh.fit.dispatchservice.entity;


import iuh.fit.dispatchservice.enums.HazardStatus;
import iuh.fit.dispatchservice.enums.HazardType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "hazard_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HazardReport {
    @Id
    private UUID id;

    @org.hibernate.annotations.Generated(event = org.hibernate.generator.EventType.INSERT)
    @org.hibernate.annotations.ColumnDefault("generate_hazard_code()")
    @Column(name = "code", nullable = false, unique = true, length = 32, updatable = false)
    private String code;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private MapPoint mapPoint;

    @Column(name = "reporter_id")
    private UUID reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "hazard_type")
    private HazardType hazardType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "image_urls", columnDefinition = "text[]")
    private List<String> imageUrls;

    @Enumerated(EnumType.STRING)
    private HazardStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }
}
