package iuh.fit.resourcemanagementservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.resourcemanagementservice.dtos.request.AllocateInventoryRequest;
import iuh.fit.resourcemanagementservice.dtos.request.CreateCampaignWarehouseInventoryRequest;
import iuh.fit.resourcemanagementservice.dtos.request.UpdateCampaignWarehouseInventoryRequest;
import iuh.fit.resourcemanagementservice.dtos.response.CampaignWarehouseInventoryResponse;
import iuh.fit.resourcemanagementservice.entity.CampaignWarehouseInventory;
import iuh.fit.resourcemanagementservice.entity.Item;
import iuh.fit.resourcemanagementservice.repositories.CampaignWarehouseInventoryRepository;
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
    private final ItemService itemService;

    /**
     * Thêm mới vật phẩm vào kho cứu trợ
     */
    @Transactional
    public CampaignWarehouseInventoryResponse createInventory(CreateCampaignWarehouseInventoryRequest request) {
        Optional<CampaignWarehouseInventory> existingOpt = inventoryRepository.findByWarehouseIdAndItem_Id(
                request.warehouseId(),
                request.itemId()
        );

        CampaignWarehouseInventory inventoryToSave;

        if (existingOpt.isPresent()) {
            CampaignWarehouseInventory existing = existingOpt.get();
            if (Boolean.TRUE.equals(existing.getIsDeleted())) {
                // Nếu đã từng bị xóa mềm (isDeleted = true), tái kích hoạt lại bản ghi với số lượng mới
                existing.setQuantity(request.quantity());
                existing.setIsDeleted(false);
            } else {
                // Nếu đã có trong kho -> Tự động CỘNG DỒN số lượng nhập thêm
                int currentQty = existing.getQuantity() != null ? existing.getQuantity() : 0;
                existing.setQuantity(currentQty + request.quantity());
            }
            inventoryToSave = existing;
        } else {
            Item item = itemService.findById(request.itemId());
            inventoryToSave = CampaignWarehouseInventory.builder()
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

        existing.setQuantity(request.quantity());

        if (request.isDeleted() != null) {
            existing.setIsDeleted(request.isDeleted());
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

    /**
     * Lấy danh sách toàn bộ vật phẩm tồn kho tại kho cứu trợ
     */
    @Transactional(readOnly = true)
    public List<CampaignWarehouseInventoryResponse> getInventoriesByWarehouse(UUID warehouseId) {
        List<CampaignWarehouseInventory> list = inventoryRepository.findByWarehouseIdAndIsDeletedFalse(warehouseId);
        return list.stream()
                .map(CampaignWarehouseInventoryResponse::fromEntity)
                .toList();
    }

    /**
     * Trừ số lượng tồn kho của một vật phẩm trong kho cứu trợ khi cấp phát
    */
    @Transactional
    public CampaignWarehouseInventory deductInventoryQuantity(UUID warehouseId, AllocateInventoryRequest.ItemAllocation itemReq) {
        CampaignWarehouseInventory warehouseInv;

        if (itemReq.warehouseInventoryId() != null) {
            warehouseInv = inventoryRepository.findByIdForUpdate(itemReq.warehouseInventoryId())
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.RESOURCE_NOT_FOUND,
                            "Không tìm thấy vật phẩm trong kho với ID: " + itemReq.warehouseInventoryId()
                    ));
        } else if (warehouseId != null && itemReq.itemId() != null) {
            warehouseInv = inventoryRepository.findByWarehouseIdAndItemIdForUpdate(warehouseId, itemReq.itemId())
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.RESOURCE_NOT_FOUND,
                            "Vật phẩm cứu trợ không tồn tại trong kho này (itemId: " + itemReq.itemId() + ")"
                    ));
        } else {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Phải cung cấp warehouseInventoryId hoặc (warehouseId và itemId) cho từng vật phẩm cấp phát"
            );
        }

        String itemName = warehouseInv.getItem() != null ? warehouseInv.getItem().getName() : "Vật phẩm";
        String itemUnit = (warehouseInv.getItem() != null && warehouseInv.getItem().getUnit() != null)
                ? warehouseInv.getItem().getUnit().name()
                : "đơn vị";

        if (Boolean.TRUE.equals(warehouseInv.getIsDeleted())) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Vật phẩm \"" + itemName + "\" không có trong kho"
            );
        }

        int currentQuantity = warehouseInv.getQuantity() != null ? warehouseInv.getQuantity() : 0;

        if (currentQuantity <= 0) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Vật phẩm \"" + itemName + "\" trong kho hiện đã hết hàng"
            );
        }

        if (currentQuantity < itemReq.quantity()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Số lượng cấp phát cho \"" + itemName + "\" vượt quá tồn kho hiện tại (Tồn kho khả dụng: "
                            + currentQuantity + " " + itemUnit
            );
        }

        warehouseInv.setQuantity(currentQuantity - itemReq.quantity());
        return inventoryRepository.save(warehouseInv);
    }
}
