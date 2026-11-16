package iuh.fit.resourcemanagementservice.controllers;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.common.response.PageResponse;
import iuh.fit.resourcemanagementservice.dtos.request.CreateItemRequest;
import iuh.fit.resourcemanagementservice.dtos.request.UpdateItemRequest;
import iuh.fit.resourcemanagementservice.dtos.response.ItemResponse;
import iuh.fit.resourcemanagementservice.services.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    /**
     * API Lấy danh sách vật phẩm (phân trang và tìm kiếm theo tên)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ItemResponse>>> getItems(
            @RequestParam(value = "keyword", required = false) String keyword,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        PageResponse<ItemResponse> response = itemService.getItems(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách vật phẩm thành công"));
    }

    /**
     * API Thêm mới Vật phẩm qua Multipart/Form-Data (hỗ trợ upload ảnh trực tiếp lên S3)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ItemResponse>> createItemWithImage(
            @Valid @ModelAttribute CreateItemRequest request,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        ItemResponse response = itemService.createItem(request, image);
        return ResponseEntity.ok(ApiResponse.success(response, "Thêm vật phẩm mới thành công"));
    }

    /**
     * API Cập nhật Vật phẩm qua Multipart/Form-Data (hỗ trợ upload ảnh mới lên S3)
     */
    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ItemResponse>> updateItemWithImage(
            @PathVariable("id") UUID id,
            @Valid @ModelAttribute UpdateItemRequest request,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        ItemResponse response = itemService.updateItem(id, request, image);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật thông tin vật phẩm thành công"));
    }

    /**
     * API Xóa mềm Vật phẩm (chuyển isDeleted = true)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable("id") UUID id
    ) {
        itemService.deleteItem(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa vật phẩm thành công"));
    }

    /**
     * API Lấy chi tiết thông tin một vật phẩm theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemResponse>> getItemById(
            @PathVariable("id") UUID id
    ) {
        ItemResponse response = itemService.getItemById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin vật phẩm thành công"));
    }
}
