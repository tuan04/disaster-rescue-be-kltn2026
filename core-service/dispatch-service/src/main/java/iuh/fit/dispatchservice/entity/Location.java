package iuh.fit.dispatchservice.entity;

import iuh.fit.dispatchservice.enums.LocationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    // 1. Nhận diện & Phân cấp hành chính
    @Column(name = "name", length = 255)
    private String name;

    // 2. Định vị không gian (GIS / Boundary)
    // Lưu ý: Yêu cầu cài đặt PostGIS dưới database và dùng Hibernate Spatial
    @Column(name = "boundary", columnDefinition = "geometry(Polygon, 4326)")
    private Polygon boundary;

    @Column(name = "center_point", columnDefinition = "geometry(Point, 4326)")
    private Point centerPoint;

    // 3. Quy tắc điều phối & Bán kính
    @Builder.Default
    @Column(name = "radius_meters", columnDefinition = "integer default 5000")
    private Integer radiusMeters = 5000;

    // 4. Trạng thái vận hành & Quản lý tải
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private LocationStatus status = LocationStatus.ACTIVE;

    @Column(name = "is_active")
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
