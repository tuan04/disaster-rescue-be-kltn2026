package iuh.fit.resourcemanagementservice.controllers;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.common.response.PageResponse;
import iuh.fit.resourcemanagementservice.dtos.request.CreateCampaignRequest;
import iuh.fit.resourcemanagementservice.dtos.request.UpdateCampaignRequest;
import iuh.fit.resourcemanagementservice.dtos.response.CampaignResponse;
import iuh.fit.resourcemanagementservice.services.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CampaignResponse>>> getCampaigns(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                campaignService.getCampaigns(keyword, page, size), "Lấy danh sách chiến dịch thành công"));
    }

    /**
     * API Tạo mới Chiến dịch (Create Campaign)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CampaignResponse>> createCampaign(
            @Valid @RequestBody CreateCampaignRequest request
    ) {
        CampaignResponse response = campaignService.createCampaign(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo chiến dịch mới thành công"));
    }

    /**
     * API Cập nhật Chiến dịch (Update Campaign)
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateCampaign(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCampaignRequest request
    ) {
        CampaignResponse response = campaignService.updateCampaign(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật chiến dịch thành công"));
    }

    /**
     * API Lấy danh sách Chiến dịch đang hoạt động (ACTIVE)
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<CampaignResponse>>> getActiveCampaigns() {
        java.util.List<CampaignResponse> response = campaignService.getActiveCampaigns();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách chiến dịch đang hoạt động thành công"));
    }
}
