package iuh.fit.resourcemanagementservice.entity;


import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "team_mobile_inventory")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TeamMobileInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_team_id")
    private CampaignTeam campaignTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(name = "current_quantity")
    private Integer currentQuantity;
}
