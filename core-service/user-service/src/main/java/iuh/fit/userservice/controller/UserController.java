package iuh.fit.userservice.controller;

import iuh.fit.userservice.dto.request.AdminUserRequest;
import iuh.fit.userservice.dto.response.AdminUserResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import iuh.fit.common.response.ApiResponse;
import iuh.fit.userservice.dto.request.UserFilterRequest;
import iuh.fit.userservice.dto.response.UserIDAndNameResponse;
import iuh.fit.userservice.dto.response.UserListResponse;
import iuh.fit.userservice.dto.response.UserProfileResponse;
import iuh.fit.userservice.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserListResponse>>> getAllUsersPage(
            @ModelAttribute UserFilterRequest request,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ){
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsersPage(request, pageable)));
    }


    @GetMapping({ "/me", "/profile" })
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("user_id"));
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


    @PostMapping
    public ResponseEntity<ApiResponse<AdminUserResponse>> createUser(
            @Validated @RequestBody AdminUserRequest request
    ) {
        AdminUserResponse response =
                userService.createUserByAdmin(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUser(
            @PathVariable("userId") UUID userId,
            @Valid @RequestBody AdminUserRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        userService.updateUser(userId, request)
                )
        );
    }




}
