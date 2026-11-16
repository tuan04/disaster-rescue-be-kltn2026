package iuh.fit.resourcemanagementservice.controllers;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.resourcemanagementservice.dtos.request.CreateCampaignWarehouseInventoryRequest;
import iuh.fit.resourcemanagementservice.dtos.request.UpdateCampaignWarehouseInventoryRequest;
import iuh.fit.resourcemanagementservice.dtos.response.CampaignWarehouseInventoryResponse;
import iuh.fit.resourcemanagementservice.services.CampaignWarehouseInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventories/warehouses")
@RequiredArgsConstructor
public class WarehouseInventoryController {

    private final CampaignWarehouseInventoryService inventoryService;

    /**
     * API Lấy danh sách vật phẩm tồn kho tại một kho cứu trợ
     */
    @GetMapping("/{warehouseId}")
    public ResponseEntity<ApiResponse<List<CampaignWarehouseInventoryResponse>>> getInventoriesByWarehouse(
            @PathVariable("warehouseId") UUID warehouseId
    ) {
        List<CampaignWarehouseInventoryResponse> response = inventoryService.getInventoriesByWarehouse(warehouseId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách vật phẩm trong kho thành công"));
    }

    /**
     * API Nhập danh sách vật phẩm vào kho chiến dịch (hỗ trợ cộng dồn tự động)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<List<CampaignWarehouseInventoryResponse>>> createInventories(
            @Valid @RequestBody CreateCampaignWarehouseInventoryRequest request
    ) {
        List<CampaignWarehouseInventoryResponse> response = inventoryService.createInventories(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Nhập vật phẩm vào kho thành công"));
    }

    /**
     * API Cập nhật thông tin tồn kho hàng loạt
     */
    @PatchMapping
    public ResponseEntity<ApiResponse<List<CampaignWarehouseInventoryResponse>>> updateInventories(
            @Valid @RequestBody List<UpdateCampaignWarehouseInventoryRequest> requests
    ) {
        List<CampaignWarehouseInventoryResponse> response = inventoryService.updateInventories(requests);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật thông tin tồn kho thành công"));
    }

    /**
     * API Xóa mềm tồn kho (isDeleted = true)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInventory(
            @PathVariable("id") UUID id
    ) {
        inventoryService.deleteInventory(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa vật phẩm khỏi kho chiến dịch thành công"));
    }
}
