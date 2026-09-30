package iuh.fit.integration.controller;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.integration.dtos.response.SMSRequest;
import iuh.fit.integration.services.SmsService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sms")
@AllArgsConstructor
public class SMSController {

    private final SmsService smsService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> receiveSmsSOS(
            @RequestBody SMSRequest smsRequest
    ) {
        smsService.processAndSendSmsSOS(smsRequest);
        return ResponseEntity.ok(ApiResponse.success(null, "SMS received successfully"));
    }

}

