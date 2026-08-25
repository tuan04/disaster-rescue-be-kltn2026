package iuh.fit.userservice.entity;

import iuh.fit.userservice.enums.RoleInTeamEnum;
import iuh.fit.userservice.enums.VerifiedStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "volunteer_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolunteerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "cccd_number", length = 12)
    private String cccdNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "team_name", columnDefinition = "TEXT")
    private String teamName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_role_in_team", length = 30)
    private RoleInTeamEnum currentRoleInTeam;

    @Enumerated(EnumType.STRING)
    @Column(name = "verified_status", length = 20)
    private VerifiedStatusEnum verifiedStatus;

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