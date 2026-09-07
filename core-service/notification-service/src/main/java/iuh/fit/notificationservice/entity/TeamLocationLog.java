package iuh.fit.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "team_location_log")
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
    @Column(name = "geom", columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point geom;

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
