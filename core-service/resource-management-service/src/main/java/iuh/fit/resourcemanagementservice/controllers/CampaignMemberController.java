package iuh.fit.resourcemanagementservice.controllers;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.resourcemanagementservice.dtos.request.AddMembersRequest;
import iuh.fit.resourcemanagementservice.dtos.response.MemberResponse;
import iuh.fit.resourcemanagementservice.services.CampaignMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/members")
@RequiredArgsConstructor
public class CampaignMemberController {

    private final CampaignMemberService campaignMemberService;

    /**
     * API Lấy danh sách thành viên của Đội Cứu hộ
     */
    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<MemberResponse>>> getTeamMembers(
            @PathVariable("teamId") UUID teamId
    ) {
        java.util.List<MemberResponse> response = campaignMemberService.getTeamMembers(teamId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách thành viên đội cứu hộ thành công"));
    }

    /**
     * API Thêm danh sách thành viên vào Đội Cứu hộ
     */
    @PostMapping
    public ResponseEntity<ApiResponse<List<MemberResponse>>> addMembers(
            @PathVariable("teamId") UUID teamId,
            @RequestHeader("X-User-Id") UUID currentUserId,
            @Valid @RequestBody AddMembersRequest request
    ) {
        List<MemberResponse> response = campaignMemberService.addMembers(teamId, currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Đã gửi lời mời tham gia đội"));
    }

    /**
     * API Xóa thành viên khỏi Đội Cứu hộ
     */
    @DeleteMapping("/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable("teamId") UUID teamId,
            @PathVariable("memberId") UUID memberId
    ) {
        campaignMemberService.removeMember(teamId, memberId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa thành viên khỏi đội cứu hộ thành công"));
    }
}
