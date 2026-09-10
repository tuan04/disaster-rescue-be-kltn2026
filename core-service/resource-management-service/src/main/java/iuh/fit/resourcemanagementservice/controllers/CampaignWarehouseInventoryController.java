package iuh.fit.resourcemanagementservice.controllers;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.resourcemanagementservice.dtos.request.CreateCampaignWarehouseInventoryRequest;
import iuh.fit.resourcemanagementservice.dtos.request.UpdateCampaignWarehouseInventoryRequest;
import iuh.fit.resourcemanagementservice.dtos.response.CampaignWarehouseInventoryResponse;
import iuh.fit.resourcemanagementservice.services.CampaignWarehouseInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaign-warehouse-inventories")
@RequiredArgsConstructor
public class CampaignWarehouseInventoryController {

    private final CampaignWarehouseInventoryService inventoryService;

    /**
     * API Thêm mới vật phẩm vào kho chiến dịch
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CampaignWarehouseInventoryResponse>> createInventory(
            @Valid @RequestBody CreateCampaignWarehouseInventoryRequest request
    ) {
        CampaignWarehouseInventoryResponse response = inventoryService.createInventory(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Thêm vật phẩm vào kho chiến dịch thành công"));
    }

    /**
     * API Cập nhật thông tin tồn kho
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CampaignWarehouseInventoryResponse>> updateInventory(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCampaignWarehouseInventoryRequest request
    ) {
        CampaignWarehouseInventoryResponse response = inventoryService.updateInventory(id, request);
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
