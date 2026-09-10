package iuh.fit.resourcemanagementservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.resourcemanagementservice.dtos.request.CreateCampaignWarehouseInventoryRequest;
import iuh.fit.resourcemanagementservice.dtos.request.UpdateCampaignWarehouseInventoryRequest;
import iuh.fit.resourcemanagementservice.dtos.response.CampaignWarehouseInventoryResponse;
import iuh.fit.resourcemanagementservice.entity.Campaign;
import iuh.fit.resourcemanagementservice.entity.CampaignWarehouseInventory;
import iuh.fit.resourcemanagementservice.entity.Item;
import iuh.fit.resourcemanagementservice.repositories.CampaignRepository;
import iuh.fit.resourcemanagementservice.repositories.CampaignWarehouseInventoryRepository;
import iuh.fit.resourcemanagementservice.repositories.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignWarehouseInventoryService {

    private final CampaignWarehouseInventoryRepository inventoryRepository;
    private final CampaignRepository campaignRepository;
    private final ItemRepository itemRepository;

    /**
     * Thêm mới vật phẩm vào kho chiến dịch
     */
    @Transactional
    public CampaignWarehouseInventoryResponse createInventory(CreateCampaignWarehouseInventoryRequest request) {
        Campaign campaign = campaignRepository.findById(request.campaignId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy chiến dịch với ID: " + request.campaignId()
                ));

        Item item = itemRepository.findByIdAndIsDeletedFalse(request.itemId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy vật phẩm hoặc vật phẩm đã bị xóa với ID: " + request.itemId()
                ));

        Optional<CampaignWarehouseInventory> existingOpt = inventoryRepository.findByCampaign_IdAndWarehouseIdAndItem_Id(
                request.campaignId(),
                request.warehouseId(),
                request.itemId()
        );

        CampaignWarehouseInventory inventoryToSave;

        if (existingOpt.isPresent()) {
            CampaignWarehouseInventory existing = existingOpt.get();
            if (Boolean.FALSE.equals(existing.getIsDeleted())) {
                throw new BusinessException(
                        ErrorCode.CONFLICT,
                        "Vật phẩm này đã tồn tại trong kho của chiến dịch"
                );
            }
            // Nếu đã từng bị xóa mềm (isDeleted = true), tái kích hoạt lại bản ghi
            existing.setQuantity(request.quantity());
            existing.setIsDeleted(false);
            inventoryToSave = existing;
        } else {
            inventoryToSave = CampaignWarehouseInventory.builder()
                    .campaign(campaign)
                    .managerPhone(request.phone())
                    .warehouseId(request.warehouseId())
                    .item(item)
                    .quantity(request.quantity())
                    .isDeleted(false)
                    .build();
        }

        CampaignWarehouseInventory saved = inventoryRepository.save(inventoryToSave);
        return CampaignWarehouseInventoryResponse.fromEntity(saved);
    }

    /**
     * Cập nhật thông tin tồn kho (số lượng, kho)
     */
    @Transactional
    public CampaignWarehouseInventoryResponse updateInventory(UUID id, UpdateCampaignWarehouseInventoryRequest request) {
        CampaignWarehouseInventory existing = inventoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy thông tin tồn kho với ID: " + id
                ));

        if (request.quantity() != null) {
            existing.setQuantity(request.quantity());
        }

        if (request.isDeleted() != null) {
            existing.setIsDeleted(request.isDeleted());
        }

        if(request.phone() != null) {
            existing.setManagerPhone(request.phone());
        }

        if (request.warehouseId() != null) {
            existing.setWarehouseId(request.warehouseId());
        }

        CampaignWarehouseInventory updated = inventoryRepository.save(existing);
        return CampaignWarehouseInventoryResponse.fromEntity(updated);
    }

    /**
     * Xóa mềm tồn kho (cập nhật isDeleted = true)
     */
    @Transactional
    public void deleteInventory(UUID id) {
        CampaignWarehouseInventory existing = inventoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy thông tin tồn kho với ID: " + id
                ));

        existing.setIsDeleted(true);
        inventoryRepository.save(existing);
    }
}
