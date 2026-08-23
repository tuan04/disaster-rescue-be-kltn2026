package iuh.fit.resourcemanagementservice.entity;

import iuh.fit.resourcemanagementservice.enums.TeamStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "campaign_teams")
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
    @JoinColumn(name = "campaign_id", insertable = false, updatable = false)
    private Campaign campaign;

    @Column(name = "total_participants")
    private Integer totalParticipants;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TeamStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    // Mapping mảng text[] / vehicle_type[] trong PostgreSQL sang List<String> Java
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "vehicles", columnDefinition = "text[]")
    @Builder.Default
    private List<String> vehicles = new ArrayList<>();

    @Column(name = "team_name")
    private String teamName;

    @Column(name = "leader_id", nullable = false)
    private UUID leaderId;

    @Column(name = "leader_phone", nullable = false, length = 15)
    private String leaderPhone;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = TeamStatus.OFFLINE;
        }
        if (totalParticipants == null) {
            totalParticipants = 0;
        }
        if (vehicles == null) {
            vehicles = new ArrayList<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }
}
