package iuh.fit.dispatchservice.entity;

import iuh.fit.dispatchservice.enums.PointType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

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

    private String address;

    @Column(name = "location", columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Column(name = "is_visible", columnDefinition = "boolean default true")
    private Boolean isVisible;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
