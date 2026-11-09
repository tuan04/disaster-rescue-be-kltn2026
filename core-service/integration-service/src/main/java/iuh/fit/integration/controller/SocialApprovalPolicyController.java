package iuh.fit.integration.controller;


import iuh.fit.common.response.ApiResponse;
import iuh.fit.integration.dtos.request.UpdateSocialApprovalPolicyRequest;
import iuh.fit.integration.entity.SocialApprovalPolicy;
import iuh.fit.integration.services.SocialApprovalPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/social-feeds/approval-policy")
@RequiredArgsConstructor
public class SocialApprovalPolicyController {

    private final SocialApprovalPolicyService policyService;


    @GetMapping
    public ResponseEntity<ApiResponse<SocialApprovalPolicy>> getPolicy() {
        SocialApprovalPolicy policy = policyService.getPolicy();
        return ResponseEntity.ok(
                ApiResponse.success(
                        policy,
                        "Lấy cấu hình tự phê duyệt thành công"
                )
        );
    }
    @PatchMapping
    public ResponseEntity<ApiResponse<SocialApprovalPolicy>> updatePolicy(
            @Valid @RequestBody UpdateSocialApprovalPolicyRequest request
    ) {
        SocialApprovalPolicy updatedPolicy = policyService.updatePolicy(
                request.enabled(),
                request.minConfidence()
        );
        return ResponseEntity.ok(
                ApiResponse.success(
                        updatedPolicy,
                        "Cập nhật cấu hình tự phê duyệt thành công"
                )
        );
    }
}
