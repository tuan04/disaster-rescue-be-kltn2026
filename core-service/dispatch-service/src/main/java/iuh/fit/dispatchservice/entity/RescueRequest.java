package iuh.fit.dispatchservice.entity;


import iuh.fit.dispatchservice.enums.EmergencyLevel;
import iuh.fit.dispatchservice.enums.RequestSource;
import iuh.fit.dispatchservice.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.*;

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

    // Chia sẻ chung ID với bảng map_points
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private MapPoint mapPoint;

    @Column(name = "reporter_id", unique = true, nullable = false)
    private UUID reporterId;

    @Column(name = "reporter_phone", length = 10)
    private String reporterPhone;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "emergency_level")
    private EmergencyLevel emergencyLevel;

    @Column(name = "feed_id")
    private UUID feedId;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    @Enumerated(EnumType.STRING)
    private RequestSource source;

}
