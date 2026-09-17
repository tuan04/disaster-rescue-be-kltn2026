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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
