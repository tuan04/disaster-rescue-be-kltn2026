package iuh.fit.dispatchservice.entity;

import iuh.fit.dispatchservice.enums.PointType;
import jakarta.persistence.*;
import lombok.*;
import org.geolatte.geom.Geometry;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "map_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MapPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "point_type")
    private PointType pointType;

    @Column(name = "location", columnDefinition = "geometry(Point, 4326)")
    private Geometry location;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
