package iuh.fit.dispatchservice.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private MapPoint mapPoint;

    private String name;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Column(name = "current_people")
    private Integer currentPeople;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "is_active", columnDefinition = "boolean default true")
    private Boolean isActive;
}
