package iuh.fit.dispatchservice.controllers;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.dispatchservice.dtos.request.CreateHazardReportRequest;
import iuh.fit.dispatchservice.dtos.request.CreateSafePointRequest;
import iuh.fit.dispatchservice.dtos.request.CreateWarehouseRequest;
import iuh.fit.dispatchservice.dtos.request.MapPointFilterRequest;
import iuh.fit.dispatchservice.dtos.response.MapPointDetailResponse;
import iuh.fit.dispatchservice.dtos.response.MapPointRes;
import iuh.fit.dispatchservice.services.MapPointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/map-points")
@RequiredArgsConstructor
public class MapPointController {
    private final MapPointService mapPointService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MapPointRes>>> getAllMapPoints(
            @ModelAttribute MapPointFilterRequest filter
    ) {
        return ResponseEntity.ok(ApiResponse.success(mapPointService.getAllMapPoints(filter)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MapPointDetailResponse>> getDetail(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(ApiResponse.success(mapPointService.getDetail(id)));
    }

    @PostMapping("/warehouses")
    public ResponseEntity<ApiResponse<MapPointDetailResponse>> createWarehouseMapPoint(
            @Valid @RequestBody CreateWarehouseRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                mapPointService.createWarehouseMapPoint(request),
                "Tạo điểm kho cứu trợ thành công"
        ));
    }


    @PostMapping("/safe-points")
    public ResponseEntity<ApiResponse<MapPointDetailResponse>> createSafeMapPoint(
            @Valid @RequestBody CreateSafePointRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                mapPointService.createSafeMapPoint(request),
                "Tạo điểm an toàn thành công"
        ));
    }


    @PostMapping(value = "/hazard-reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MapPointDetailResponse>> createHazardReports(
            @ModelAttribute @Valid CreateHazardReportRequest request,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestHeader(value = "X-User-Id", required = false) UUID reporterId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                mapPointService.createHazardReport(request, images, reporterId),
                "Báo cáo hiểm họa thành công"
        ));
    }



}
