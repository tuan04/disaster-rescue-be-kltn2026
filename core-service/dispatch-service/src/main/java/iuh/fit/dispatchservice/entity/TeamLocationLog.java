package iuh.fit.dispatchservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.LineString;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "team_location_log",
        indexes = {
                @Index(name = "idx_tll_campaign_team_id", columnList = "campaign_team_id"),
                @Index(name = "idx_tll_assignment_id", columnList = "assignment_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamLocationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "location_log_seq")
    @SequenceGenerator(name = "location_log_seq", sequenceName = "team_location_log_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "campaign_team_id", nullable = false)
    private UUID campaignTeamId;

    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @Column(name = "geom", columnDefinition = "geometry(LineString, 4326)", nullable = false)
    private LineString lineString;

    @Column(name = "distance_meters", nullable = false)
    private Double distanceMeters;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }
}
