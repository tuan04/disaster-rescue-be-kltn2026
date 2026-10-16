package iuh.fit.dispatchservice.controllers;


import iuh.fit.common.kafka.dto.SOSRequest;
import iuh.fit.common.kafka.dto.SOSResponse;
import iuh.fit.common.response.ApiResponse;
import iuh.fit.dispatchservice.dtos.request.UpdateSOSRequest;
import iuh.fit.dispatchservice.services.SOSService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sos-requests")
@RequiredArgsConstructor
public class SOSController {
    private final SOSService sosService;

    @PostMapping
    public ResponseEntity<ApiResponse<SOSResponse>> createSOSRequest(
            @Validated @RequestBody SOSRequest sosRequest,
            @AuthenticationPrincipal Jwt jwt
    ){
        UUID userId = jwt == null
                ? null
                : UUID.fromString(jwt.getClaimAsString("id"));
        SOSResponse sosResponse = sosService.createSOSRequest(sosRequest, userId);
        return ResponseEntity.ok(ApiResponse.success(sosResponse));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<SOSResponse>>  updateSOSRequest(
            @Validated @RequestBody UpdateSOSRequest updateSOSRequest
    ){
        return ResponseEntity.ok(ApiResponse.success(sosService.updateSOSRequest(updateSOSRequest)));
    }
}
