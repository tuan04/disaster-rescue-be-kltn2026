package iuh.fit.userservice.controller;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.userservice.dto.request.UpgradeRescuerRequest;
import iuh.fit.userservice.dto.response.UserIDAndNameResponse;
import iuh.fit.userservice.dto.response.UserProfileResponse;
import iuh.fit.userservice.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping({ "/me", "/profile" })
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserProfile(userId)));
    }

    // trả về id và name
    @GetMapping("/names")
    public ResponseEntity<ApiResponse<List<UserIDAndNameResponse>>> getUserNames() {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserNames()));
    }

    @PatchMapping(value = "/upgrade-rescuer-request/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> upgradeToRescuer(
            @RequestParam("cccdImage") MultipartFile file,
            @PathVariable("userId") UUID userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(userService.upgradeToRescuerRequest(userId, file)));
    }

    @PatchMapping("/upgrade-rescuer-accept/{id}")
    public ResponseEntity<ApiResponse<?>> upgradeToRescuerAccept(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(ApiResponse.success(userService.upgradeToRescuerRequestAccept(id)));
    }
}
