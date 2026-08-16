package iuh.fit.dispatchservice.controllers;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.dispatchservice.dtos.request.MapPointFilterRequest;
import iuh.fit.dispatchservice.dtos.response.MapPointDetailResponse;
import iuh.fit.dispatchservice.dtos.response.MapPointRes;
import iuh.fit.dispatchservice.services.MapPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
