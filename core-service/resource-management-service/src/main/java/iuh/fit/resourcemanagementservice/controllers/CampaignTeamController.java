package iuh.fit.resourcemanagementservice.controllers;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.resourcemanagementservice.dtos.redis.TeamLocation;
import iuh.fit.resourcemanagementservice.dtos.request.TeamLocationRequest;
import iuh.fit.resourcemanagementservice.dtos.request.CreateTeamRequest;
import iuh.fit.resourcemanagementservice.dtos.request.UpdateTeamRequest;
import iuh.fit.resourcemanagementservice.dtos.response.TeamResponse;
import iuh.fit.resourcemanagementservice.services.CampaignTeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaign-teams")
@RequiredArgsConstructor
public class CampaignTeamController {

    private final CampaignTeamService campaignTeamService;

    /**
     * API Tạo mới Đội Cứu hộ (Create Campaign Team)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TeamResponse>> createCampaignTeam(
            @Valid @RequestBody CreateTeamRequest request) {
        TeamResponse response = campaignTeamService.createTeam(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo đội cứu hộ mới thành công"));
    }

    /**
     * API Cập nhật Đội Cứu hộ (Update Campaign Team)
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamResponse>> updateCampaignTeam(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateTeamRequest request) {
        TeamResponse response = campaignTeamService.updateTeam(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật thông tin đội cứu hộ thành công"));
    }

    @PostMapping("/location")
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            @Valid @RequestBody TeamLocationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID leaderId) {
        campaignTeamService.saveTeamLocation(leaderId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}/location")
    public ResponseEntity<ApiResponse<TeamLocation>> getTeamLocation(
            @PathVariable("id") UUID id) {
        TeamLocation response = campaignTeamService.getTeamLocation(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy vị trí đội cứu hộ thành công"));
    }
}
