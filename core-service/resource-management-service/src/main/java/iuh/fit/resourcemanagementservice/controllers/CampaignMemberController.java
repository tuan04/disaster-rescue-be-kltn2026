package iuh.fit.resourcemanagementservice.controllers;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.resourcemanagementservice.dtos.request.AddMemberRequest;
import iuh.fit.resourcemanagementservice.dtos.response.MemberResponse;
import iuh.fit.resourcemanagementservice.services.CampaignMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/members")
@RequiredArgsConstructor
public class CampaignMemberController {

    private final CampaignMemberService campaignMemberService;

    /**
     * API Thêm thành viên vào Đội Cứu hộ
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MemberResponse>> addMember(
            @PathVariable("teamId") UUID teamId,
            @Valid @RequestBody AddMemberRequest request
    ) {
        MemberResponse response = campaignMemberService.addMember(teamId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Thêm thành viên vào đội cứu hộ thành công"));
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
