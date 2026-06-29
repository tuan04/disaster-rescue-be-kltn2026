package iuh.fit.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.geolatte.geom.Geometry;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "social_raw_feed")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamLocationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "campaign_team_id", nullable = false)
    private UUID campaignTeamId;
    @Column(name = "geom", columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Geometry geom;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}
