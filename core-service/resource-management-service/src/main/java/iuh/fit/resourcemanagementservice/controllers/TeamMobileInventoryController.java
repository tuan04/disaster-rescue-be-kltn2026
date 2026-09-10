package iuh.fit.resourcemanagementservice.controllers;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.resourcemanagementservice.dtos.request.CreateTeamMobileInventoryRequest;
import iuh.fit.resourcemanagementservice.dtos.request.UpdateTeamMobileInventoryRequest;
import iuh.fit.resourcemanagementservice.dtos.response.TeamMobileInventoryResponse;
import iuh.fit.resourcemanagementservice.services.TeamMobileInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/team-mobile-inventories")
@RequiredArgsConstructor
public class TeamMobileInventoryController {

    private final TeamMobileInventoryService inventoryService;

    /**
     * API Thêm mới vật phẩm vào kho lưu động của đội cứu hộ
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TeamMobileInventoryResponse>> createInventory(
            @Valid @RequestBody CreateTeamMobileInventoryRequest request
    ) {
        TeamMobileInventoryResponse response = inventoryService.createInventory(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Thêm vật phẩm vào kho lưu động thành công"));
    }

    /**
     * API Cập nhật thông tin tồn kho lưu động
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamMobileInventoryResponse>> updateInventory(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateTeamMobileInventoryRequest request
    ) {
        TeamMobileInventoryResponse response = inventoryService.updateInventory(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật thông tin tồn kho lưu động thành công"));
    }

    /**
     * API Xóa mềm tồn kho lưu động (isDeleted = true)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInventory(
            @PathVariable("id") UUID id
    ) {
        inventoryService.deleteInventory(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa vật phẩm khỏi kho lưu động thành công"));
    }


}
