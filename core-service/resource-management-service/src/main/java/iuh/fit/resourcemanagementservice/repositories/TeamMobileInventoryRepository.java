package iuh.fit.resourcemanagementservice.repositories;

import iuh.fit.resourcemanagementservice.entity.TeamMobileInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamMobileInventoryRepository extends JpaRepository<TeamMobileInventory, UUID> {

    @EntityGraph(attributePaths = {"campaignTeam", "item"})
    Optional<TeamMobileInventory> findByIdAndIsDeletedFalse(UUID id);

    @EntityGraph(attributePaths = {"campaignTeam", "item"})
    Optional<TeamMobileInventory> findByCampaignTeam_IdAndItem_Id(UUID campaignTeamId, UUID itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"campaignTeam", "item"})
    @Query("SELECT t FROM TeamMobileInventory t WHERE t.campaignTeam.id = :teamId AND t.item.id = :itemId")
    Optional<TeamMobileInventory> findByCampaignTeamIdAndItemIdForUpdate(@Param("teamId") UUID teamId, @Param("itemId") UUID itemId);

    @EntityGraph(attributePaths = {"campaignTeam", "item"})
    List<TeamMobileInventory> findByCampaignTeam_IdAndIsDeletedFalse(UUID campaignTeamId);
}
