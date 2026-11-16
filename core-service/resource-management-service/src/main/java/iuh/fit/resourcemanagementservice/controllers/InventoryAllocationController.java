package iuh.fit.resourcemanagementservice.controllers;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.resourcemanagementservice.dtos.request.AllocateInventoryRequest;
import iuh.fit.resourcemanagementservice.dtos.response.TeamMobileInventoryResponse;
import iuh.fit.resourcemanagementservice.services.InventoryAllocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
public class InventoryAllocationController {

    private final InventoryAllocationService allocationService;

    /**
     * API Cấp phát vật tư từ Kho cứu trợ cho Đội cứu hộ
     */
    @PostMapping("/allocate")
    public ResponseEntity<ApiResponse<List<TeamMobileInventoryResponse>>> allocateInventory(
            @Valid @RequestBody AllocateInventoryRequest request
    ) {
        List<TeamMobileInventoryResponse> response = allocationService.allocateInventory(request);
        return ResponseEntity.ok(ApiResponse.success(
                response,
                String.format("Cấp phát thành công %d mặt hàng cho đội cứu hộ", response.size())
        ));
    }
}
