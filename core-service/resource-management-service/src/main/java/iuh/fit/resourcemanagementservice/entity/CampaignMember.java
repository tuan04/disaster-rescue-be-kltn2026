package iuh.fit.resourcemanagementservice.entity;

import iuh.fit.resourcemanagementservice.enums.SexEnum;
import iuh.fit.resourcemanagementservice.enums.TeamRole;
import iuh.fit.resourcemanagementservice.enums.MembershipStatus;
import iuh.fit.resourcemanagementservice.enums.MembershipType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "campaign_members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_campaign_team_member", columnNames = {"campaign_team_id", "member_id"})
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CampaignMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_team_id")
    private CampaignTeam campaignTeam;

    @Column(name = "member_id")
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_team", nullable = false, length = 20)
    private TeamRole roleInTeam;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", nullable = false, length = 20)
    private MembershipStatus membershipStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_type", nullable = false, length = 20)
    private MembershipType membershipType;

    @Column(length = 20, nullable = false)
    private String phone;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private SexEnum sex;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @PrePersist
    protected void onCreate() {
        if (membershipStatus == null) {
            membershipStatus = MembershipStatus.ACCEPTED;
        }
        if (membershipType == null) {
            membershipType = memberId == null ? MembershipType.GUEST : MembershipType.REGISTERED;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }
}
