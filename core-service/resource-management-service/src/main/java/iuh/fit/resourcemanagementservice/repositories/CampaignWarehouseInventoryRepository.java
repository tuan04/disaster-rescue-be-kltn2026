package iuh.fit.resourcemanagementservice.repositories;

import iuh.fit.resourcemanagementservice.entity.CampaignWarehouseInventory;
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
public interface CampaignWarehouseInventoryRepository extends JpaRepository<CampaignWarehouseInventory, UUID> {

    @EntityGraph(attributePaths = {"item"})
    Optional<CampaignWarehouseInventory> findByIdAndIsDeletedFalse(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"item"})
    @Query("SELECT c FROM CampaignWarehouseInventory c WHERE c.id = :id AND c.isDeleted = false")
    Optional<CampaignWarehouseInventory> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"item"})
    @Query("SELECT c FROM CampaignWarehouseInventory c WHERE c.warehouseId = :warehouseId AND c.item.id = :itemId AND c.isDeleted = false")
    Optional<CampaignWarehouseInventory> findByWarehouseIdAndItemIdForUpdate(@Param("warehouseId") UUID warehouseId, @Param("itemId") UUID itemId);

    @EntityGraph(attributePaths = {"item"})
    Optional<CampaignWarehouseInventory> findByWarehouseIdAndItem_Id(UUID warehouseId, UUID itemId);

    @EntityGraph(attributePaths = {"item"})
    Optional<CampaignWarehouseInventory> findByWarehouseIdAndItem_IdAndIsDeletedFalse(UUID warehouseId, UUID itemId);

    @EntityGraph(attributePaths = {"item"})
    List<CampaignWarehouseInventory> findByWarehouseIdAndIsDeletedFalse(UUID warehouseId);
}
