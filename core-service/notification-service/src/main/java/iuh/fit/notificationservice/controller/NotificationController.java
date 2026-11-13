package iuh.fit.notificationservice.controller;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.common.response.PageResponse;
import iuh.fit.notificationservice.dtos.NotificationResponse;
import iuh.fit.notificationservice.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Lấy danh sách thông báo của người dùng hiện tại có phân trang (chưa bị xóa)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getAllNotifications(
            // Spring cung cấp JWT đã được xác thực.
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("id"));
        PageResponse<NotificationResponse> response = notificationService.getNotificationsByUserId(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Đánh dấu 1 thông báo cụ thể là đã đọc
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-User-Id") UUID userId
    ) {
        UUID id = UUID.fromString(jwt.getClaimAsString("id"));
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Đánh dấu tất cả thông báo của người dùng là đã đọc
     */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("id"));
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Xóa 1 thông báo cụ thể (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> markAsDeleted(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("id"));
        notificationService.markAsDeleted(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Xóa tất cả thông báo của người dùng (soft delete)
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> markAllAsDeleted(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("id"));
        notificationService.markAllAsDeleted(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
