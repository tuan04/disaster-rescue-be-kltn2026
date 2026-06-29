package iuh.fit.resourcemanagementservice.entity;


import iuh.fit.resourcemanagementservice.enums.TeamRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "campaign_members")
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
    @Column(name = "role_in_team")
    private TeamRole roleInTeam;
}
