package iuh.fit.dispatchservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {
    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private MapPoint mapPoint;

    private String name;

    @Column(name = "manager_phone")
    private String managerPhone;

    @Column(name = "is_active", columnDefinition = "boolean default true")
    private Boolean isActive;
}
