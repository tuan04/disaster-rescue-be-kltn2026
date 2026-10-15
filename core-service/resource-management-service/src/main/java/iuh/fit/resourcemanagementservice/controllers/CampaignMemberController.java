package iuh.fit.resourcemanagementservice.controllers;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.resourcemanagementservice.dtos.request.AddMembersRequest;
import iuh.fit.resourcemanagementservice.dtos.response.MemberResponse;
import iuh.fit.resourcemanagementservice.dtos.response.TeamInvitationResponse;
import iuh.fit.resourcemanagementservice.services.CampaignMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaign-members")
@RequiredArgsConstructor
public class CampaignMemberController {

    private final CampaignMemberService campaignMemberService;

    /**
     * API Lấy danh sách thành viên của Đội Cứu hộ
     */
    @GetMapping("/team/{teamId}")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getTeamMembers(
            @PathVariable("teamId") UUID teamId
    ) {
        List<MemberResponse> response = campaignMemberService.getTeamMembers(teamId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách thành viên đội cứu hộ thành công"));
    }

    /**
     * API Thêm danh sách thành viên vào Đội Cứu hộ
     */
    @PostMapping("/team/{teamId}")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> addMembers(
            @PathVariable("teamId") UUID teamId,
            @RequestHeader(value = "X-User-Id", required = false) UUID currentUserId,
            @Valid @RequestBody AddMembersRequest request
    ) {
        List<MemberResponse> response = campaignMemberService.addMembers(teamId, currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Đã gửi lời mời tham gia đội"));
    }

    /**
     * API Xóa thành viên khỏi Đội Cứu hộ
     */
    @DeleteMapping("/team/{teamId}/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable("teamId") UUID teamId,
            @PathVariable("memberId") UUID memberId
    ) {
        campaignMemberService.removeMember(teamId, memberId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa thành viên khỏi đội cứu hộ thành công"));
    }

    /**
     * API Cứu hộ viên xem danh sách các lời mời tham gia đội của mình
     */
    @GetMapping("/my-invitations")
    public ResponseEntity<ApiResponse<List<TeamInvitationResponse>>> getMyInvitations(
            @RequestHeader(value = "X-User-Id", required = false) UUID rescuerId
    ) {
        List<TeamInvitationResponse> response = campaignMemberService.getMyInvitations(rescuerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách lời mời thành công"));
    }

    /**
     * API Cứu hộ viên chấp nhận lời mời tham gia đội
     */
    @PostMapping("/invitations/{membershipId}/accept")
    public ResponseEntity<ApiResponse<MemberResponse>> acceptInvitation(
            @PathVariable("membershipId") UUID membershipId,
            @RequestHeader(value = "X-User-Id", required = false) UUID rescuerId
    ) {
        MemberResponse response = campaignMemberService.acceptInvitation(membershipId, rescuerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã tham gia đội cứu hộ thành công"));
    }

    /**
     * API Cứu hộ viên từ chối lời mời tham gia đội
     */
    @PostMapping("/invitations/{membershipId}/decline")
    public ResponseEntity<ApiResponse<MemberResponse>> declineInvitation(
            @PathVariable("membershipId") UUID membershipId,
            @RequestHeader(value = "X-User-Id", required = false) UUID rescuerId
    ) {
        MemberResponse response = campaignMemberService.declineInvitation(membershipId, rescuerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã từ chối lời mời tham gia đội cứu hộ"));
    }
}
