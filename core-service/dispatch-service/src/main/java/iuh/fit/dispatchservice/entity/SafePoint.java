package iuh.fit.dispatchservice.entity;

import iuh.fit.dispatchservice.enums.SafePointType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "safe_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafePoint {
    @Id
    private UUID id;

    @org.hibernate.annotations.Generated(event = org.hibernate.generator.EventType.INSERT)
    @org.hibernate.annotations.ColumnDefault("generate_safe_point_code()")
    @Column(name = "code", nullable = false, unique = true, length = 32, updatable = false)
    private String code;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private MapPoint mapPoint;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "safe_point_type")
    private SafePointType safePointType;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "is_active", columnDefinition = "boolean default true")
    private Boolean isActive;

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
