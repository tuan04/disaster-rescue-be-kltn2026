package iuh.fit.dispatchservice.controllers;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.dispatchservice.dtos.response.RouteResponse;
import iuh.fit.dispatchservice.services.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    public ResponseEntity<ApiResponse<RouteResponse>> getRoute(
            @RequestParam Double startLat,
            @RequestParam Double startLng,
            @RequestParam UUID requestId,
            @RequestParam(required = false, defaultValue = "driving") String profile) {
        RouteResponse route = routeService.getRoute(startLat, startLng, requestId, profile);
        return ResponseEntity.ok(ApiResponse.success(route));
    }
}
