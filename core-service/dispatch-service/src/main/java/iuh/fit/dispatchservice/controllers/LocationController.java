package iuh.fit.dispatchservice.controllers;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.dispatchservice.dtos.response.LocationPageResponse;
import iuh.fit.dispatchservice.dtos.response.LocationResponse;
import iuh.fit.dispatchservice.enums.LocationStatus;
import iuh.fit.dispatchservice.services.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import iuh.fit.dispatchservice.dtos.request.CreateLocationRequest;
import iuh.fit.dispatchservice.dtos.request.UpdateLocationRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {
    private final LocationService locationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getAllLocations() {
        List<LocationResponse> locations = locationService.getAllLocations();
        return ResponseEntity.ok(ApiResponse.success(locations));
    }

    @GetMapping("/pages")
    public ResponseEntity<ApiResponse<Page<LocationPageResponse>>> getAllLocationsWithPagination(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) LocationStatus status,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        Page<LocationPageResponse> pageResult = locationService.getAllLocationsWithPagination(
                isActive,
                userId,
                status,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.success(pageResult, "Lấy danh sách địa điểm thành công"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LocationPageResponse>> createLocation(
            @Valid @RequestBody CreateLocationRequest request
    ) {
        LocationPageResponse response = locationService.createLocation(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo khu vực thành công"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<LocationPageResponse>> updateLocation(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLocationRequest request
    ) {
        LocationPageResponse response = locationService.updateLocation(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật khu vực thành công"));
    }
}
