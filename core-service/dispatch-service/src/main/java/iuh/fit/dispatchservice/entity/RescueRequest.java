package iuh.fit.dispatchservice.entity;


import iuh.fit.dispatchservice.enums.EmergencyLevel;
import iuh.fit.dispatchservice.enums.RequestSource;
import iuh.fit.dispatchservice.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rescue_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescueRequest {
    @Id
    private UUID id;

    @org.hibernate.annotations.Generated(event = org.hibernate.generator.EventType.INSERT)
    @org.hibernate.annotations.ColumnDefault("generate_sos_code()")
    @Column(name = "code", nullable = false, unique = true, length = 32, updatable = false)
    private String code;

    // Chia sẻ chung ID với bảng map_points
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private MapPoint mapPoint;

    @Column(name = "reporter_id", nullable = true)
    private UUID reporterId;

    @Column(name = "reporter_phone", length = 10)
    private String reporterPhone;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "emergency_level")
    private EmergencyLevel emergencyLevel;

    @Column(name = "complete_at")
    private LocalDateTime completeAt;

    @Column(name = "safe_at")
    private LocalDateTime safeAt;


    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    @Enumerated(EnumType.STRING)
    private RequestSource source;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "ai_evaluation", columnDefinition = "TEXT")
    private String aiEvaluation;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }
}
