package iuh.fit.resourcemanagementservice.controllers;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.resourcemanagementservice.dtos.redis.TeamLocation;
import iuh.fit.resourcemanagementservice.dtos.request.TeamLocationRequest;
import iuh.fit.resourcemanagementservice.dtos.request.CreateTeamByAdminRequest;
import iuh.fit.resourcemanagementservice.dtos.request.CreateTeamByLeaderRequest;
import iuh.fit.resourcemanagementservice.enums.TeamApprovalStatus;
import iuh.fit.resourcemanagementservice.dtos.request.RejectTeamRequest;
import iuh.fit.resourcemanagementservice.dtos.request.UpdateTeamRequest;
import iuh.fit.resourcemanagementservice.dtos.response.TeamResponse;
import iuh.fit.resourcemanagementservice.dtos.response.TeamSummaryResponse;
import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.services.CampaignTeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import iuh.fit.common.response.PageResponse;
import iuh.fit.resourcemanagementservice.enums.TeamStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaign-teams")
@RequiredArgsConstructor
public class CampaignTeamController {

    private final CampaignTeamService campaignTeamService;

    @PostMapping
    public ResponseEntity<ApiResponse<TeamResponse>> createTeamByLeader(
            @Valid @RequestBody CreateTeamByLeaderRequest request,
            @RequestHeader("X-User-Id") UUID leaderId) {
        TeamResponse response = campaignTeamService.createTeam(
                request.campaignId(), request.teamName(), request.vehicles(),
                leaderId, leaderId, TeamApprovalStatus.PENDING);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Đã gửi yêu cầu thành lập đội"));
    }

    @PostMapping("/admin")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeamByAdmin(
            @Valid @RequestBody CreateTeamByAdminRequest request,
            @RequestHeader("X-User-Id") UUID adminId) {
        TeamResponse response = campaignTeamService.createTeam(
                request.campaignId(), request.teamName(), request.vehicles(),
                request.leaderId(), adminId, TeamApprovalStatus.APPROVED);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo đội cứu hộ thành công"));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<TeamResponse>> approveTeam(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") UUID reviewerId) {
        TeamResponse response = campaignTeamService.approveTeam(id, reviewerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Duyệt yêu cầu thành lập đội thành công"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<TeamResponse>> rejectTeam(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") UUID reviewerId,
            @Valid @RequestBody RejectTeamRequest request) {
        TeamResponse response = campaignTeamService.rejectTeam(id, reviewerId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Từ chối yêu cầu thành lập đội thành công"));
    }

    /**
     * API Lấy danh sách đội cứu hộ đang hoạt động trong các chiến dịch hiện tại (Dành cho Người dùng / Mobile)
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<PageResponse<TeamSummaryResponse>>> getActiveTeams(
            @RequestParam(required = false) String keyword,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<TeamSummaryResponse> response = campaignTeamService.getActiveTeams(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách đội cứu hộ đang hoạt động thành công"));
    }

    /**
     * API Lấy danh sách Đội cứu hộ (phân trang, lọc theo chiến dịch, trạng thái duyệt, trạng thái hoạt động, tìm kiếm)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TeamSummaryResponse>>> getTeams(
            @RequestParam(required = false) UUID campaignId,
            @RequestParam(required = false) TeamApprovalStatus approvalStatus,
            @RequestParam(required = false) TeamStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<TeamSummaryResponse> response = campaignTeamService.getTeams(campaignId, approvalStatus, status, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách đội cứu hộ thành công"));
    }

    /**
     * API Lấy thông tin đội cứu hộ đang hoạt động của người dùng hiện tại (Đội trưởng hoặc Cứu hộ viên)
     */
    @GetMapping("/my-active")
    public ResponseEntity<ApiResponse<TeamResponse>> getMyActiveTeam(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        TeamResponse response = campaignTeamService.getMyActiveTeam(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin đội cứu hộ đang hoạt động thành công"));
    }

    /**
     * API Lấy chi tiết thông tin Đội Cứu hộ theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(
            @PathVariable("id") UUID id) {
        TeamResponse response = campaignTeamService.getTeamById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin đội cứu hộ thành công"));
    }

    /**
     * API "Off team" (Giải tán / Kết thúc đợt cứu trợ của Đội cứu hộ)
     */
    @PostMapping("/{id}/disband")
    public ResponseEntity<ApiResponse<TeamResponse>> disbandTeam(
            @PathVariable("id") UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID leaderId) {
        TeamResponse response = campaignTeamService.disbandTeam(id, leaderId);
        return ResponseEntity.ok(ApiResponse.success(response, "Giải tán đội cứu hộ thành công"));
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


    @GetMapping("/locations-active")
    public ResponseEntity<ApiResponse<List<TeamLocation>>> getAllTeamLocationsActive() {
        List<TeamLocation> locations = campaignTeamService.getAllTeamLocations();
        return ResponseEntity.ok(ApiResponse.success(locations, "Lấy danh sách vị trí đội cứu hộ thành công"));
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<CampaignTeam>>> getAllTeamLocationsNearBy(
            @RequestParam(name = "latitude", required = true) Double latitude,
            @RequestParam(name = "longitude", required = true) Double longitude,
            @RequestParam(name = "radiusInMeters", required = true) Integer  radiusInMeters
    ) {
        List<CampaignTeam> locations = campaignTeamService.getTeamsLocationsNearby(latitude, longitude, radiusInMeters);
        return ResponseEntity.ok(ApiResponse.success(locations, "Lấy danh sách vị trí đội cứu hộ gần đây thành công"));
    }
}
