package iuh.fit.resourcemanagementservice.repositories;

import iuh.fit.resourcemanagementservice.entity.CampaignWarehouseInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignWarehouseInventoryRepository extends JpaRepository<CampaignWarehouseInventory, UUID> {

    Optional<CampaignWarehouseInventory> findByIdAndIsDeletedFalse(UUID id);

    Optional<CampaignWarehouseInventory> findByCampaign_IdAndWarehouseIdAndItem_Id(UUID campaignId, UUID warehouseId, UUID itemId);
}
