package iuh.fit.resourcemanagementservice.repositories;

import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignTeamRepository extends JpaRepository<CampaignTeam, UUID> {
    Optional<CampaignTeam> findByLeaderId(UUID leaderId);
}
