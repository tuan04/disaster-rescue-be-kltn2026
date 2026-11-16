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
@RequestMapping("/api/v1/inventories/teams")
@RequiredArgsConstructor
public class TeamInventoryController {

    private final TeamMobileInventoryService inventoryService;

    /**
     * API Lấy danh sách toàn bộ vật phẩm trong kho lưu động của một đội cứu hộ
     */
    @GetMapping("/{teamId}")
    public ResponseEntity<ApiResponse<List<TeamMobileInventoryResponse>>> getInventoriesByTeam(
            @PathVariable("teamId") UUID teamId
    ) {
        List<TeamMobileInventoryResponse> response = inventoryService.getInventoriesByTeam(teamId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách vật phẩm của đội cứu hộ thành công"));
    }

    /**
     * API Bổ sung danh sách vật phẩm vào kho lưu động của đội cứu hộ (hỗ trợ cộng dồn tự động)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<List<TeamMobileInventoryResponse>>> createInventories(
            @Valid @RequestBody CreateTeamMobileInventoryRequest request
    ) {
        List<TeamMobileInventoryResponse> response = inventoryService.createInventories(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Thêm vật phẩm vào kho lưu động thành công"));
    }

    /**
     * API Cập nhật thông tin tồn kho lưu động hàng loạt
     */
    @PatchMapping
    public ResponseEntity<ApiResponse<List<TeamMobileInventoryResponse>>> updateInventories(
            @Valid @RequestBody List<UpdateTeamMobileInventoryRequest> requests
    ) {
        List<TeamMobileInventoryResponse> response = inventoryService.updateInventories(requests);
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
