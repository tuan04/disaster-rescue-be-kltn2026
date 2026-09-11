package iuh.fit.resourcemanagementservice.repositories;

import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignTeamRepository extends JpaRepository<CampaignTeam, UUID> {
    @Query("""
        SELECT ct
        FROM  CampaignTeam ct JOIN Campaign c
        ON ct.campaign.id = c.id
        WHERE c.status = 'ACTIVE'
        AND ct.leaderId = :leaderId
""")
    Optional<CampaignTeam> findActiveTeamByLeaderId(
            UUID leaderId
    );

    boolean existsByIdAndLeaderId(UUID id, UUID leaderId);
}
