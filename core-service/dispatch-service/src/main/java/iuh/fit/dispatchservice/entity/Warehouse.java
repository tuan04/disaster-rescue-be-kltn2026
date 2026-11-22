package iuh.fit.dispatchservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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

    @org.hibernate.annotations.Generated(event = org.hibernate.generator.EventType.INSERT)
    @org.hibernate.annotations.ColumnDefault("generate_warehouse_code()")
    @Column(name = "code", nullable = false, unique = true, length = 32, updatable = false)
    private String code;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private MapPoint mapPoint;

    private String name;

    @Column(name = "manager_phone")
    private String managerPhone;

    @Column(name = "is_active", columnDefinition = "boolean default true")
    private Boolean isActive;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }
}
