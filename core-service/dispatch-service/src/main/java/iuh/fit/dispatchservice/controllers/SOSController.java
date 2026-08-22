package iuh.fit.dispatchservice.controllers;


import iuh.fit.common.response.ApiResponse;
import iuh.fit.dispatchservice.dtos.request.SOSRequest;
import iuh.fit.dispatchservice.dtos.response.SOSResponse;
import iuh.fit.dispatchservice.services.SOSService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sos-requests")
@RequiredArgsConstructor
public class SOSController {
    private final SOSService sosService;

    @PostMapping
    public ResponseEntity<ApiResponse<SOSResponse>> createSOSRequest(
            @Validated @RequestBody SOSRequest sosRequest,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ){
        System.out.println("Received userId: " + userId);
        SOSResponse sosResponse = sosService.createSOSRequest(sosRequest, userId);
        return ResponseEntity.ok(ApiResponse.success(sosResponse));
    }
}
