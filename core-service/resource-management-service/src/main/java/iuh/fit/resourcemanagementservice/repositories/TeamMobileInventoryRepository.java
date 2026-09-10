package iuh.fit.resourcemanagementservice.repositories;

import iuh.fit.resourcemanagementservice.entity.TeamMobileInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamMobileInventoryRepository extends JpaRepository<TeamMobileInventory, UUID> {

    Optional<TeamMobileInventory> findByIdAndIsDeletedFalse(UUID id);

    Optional<TeamMobileInventory> findByCampaignTeam_IdAndItem_Id(UUID campaignTeamId, UUID itemId);
}
