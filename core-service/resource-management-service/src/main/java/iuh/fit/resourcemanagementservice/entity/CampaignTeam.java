package iuh.fit.resourcemanagementservice.entity;


import iuh.fit.resourcemanagementservice.enums.VehicleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "items")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CampaignTeam {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private RescueTeam rescueTeam;

    @Column(name = "total_participants")
    private Integer totalParticipants;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Mapping mảng Enum của PostgreSQL sang List Java
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "vehicles", columnDefinition = "text[]")
    private List<VehicleType> vehicles;
}
