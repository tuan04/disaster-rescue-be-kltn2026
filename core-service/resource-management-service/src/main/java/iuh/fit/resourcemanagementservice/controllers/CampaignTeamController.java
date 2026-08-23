package iuh.fit.resourcemanagementservice.controllers;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.resourcemanagementservice.dtos.request.CreateTeamRequest;
import iuh.fit.resourcemanagementservice.dtos.request.UpdateTeamRequest;
import iuh.fit.resourcemanagementservice.dtos.response.TeamResponse;
import iuh.fit.resourcemanagementservice.services.CampaignTeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
            @Valid @RequestBody CreateTeamRequest request
    ) {
        TeamResponse response = campaignTeamService.createTeam(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo đội cứu hộ mới thành công"));
    }

    /**
     * API Cập nhật Đội Cứu hộ (Update Campaign Team)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamResponse>> updateCampaignTeam(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateTeamRequest request
    ) {
        TeamResponse response = campaignTeamService.updateTeam(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật thông tin đội cứu hộ thành công"));
    }
}
