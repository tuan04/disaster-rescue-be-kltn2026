package iuh.fit.dispatchservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.dispatchservice.dtos.projection.HazardSpatialProjection;
import iuh.fit.dispatchservice.dtos.response.RouteResponse;
import iuh.fit.dispatchservice.entity.RescueRequest;
import iuh.fit.dispatchservice.repositories.HazardReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class RouteService {
    private final RestClient goongRestClient;
    private final RescueService rescueService;
    private final HazardReportRepository hazardReportRepository;
    private final RouteSafetyEngine routeSafetyEngine;
    private final String apiKey;

    public RouteService(
            RestClient.Builder restClientBuilder,
            RescueService rescueService,
            HazardReportRepository hazardReportRepository,
            RouteSafetyEngine routeSafetyEngine,
            @Value("${goong.base-url:https://rsapi.goong.io}") String baseUrl,
            @Value("${goong.api-key:}") String apiKey) {
        this.goongRestClient = restClientBuilder.baseUrl(baseUrl).build();
        this.rescueService = rescueService;
        this.hazardReportRepository = hazardReportRepository;
        this.routeSafetyEngine = routeSafetyEngine;
        this.apiKey = apiKey;
    }

    public RouteResponse getRoute(Double startLat, Double startLng, UUID requestId, String vehicle) {
        final String finalVehicle = (vehicle == null) ? "car" : vehicle.trim().toLowerCase();

        RescueRequest rescueRequest = rescueService.getRescueRequest(requestId);
        if (rescueRequest.getMapPoint() == null || rescueRequest.getMapPoint().getLocation() == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy vị trí yêu cầu cứu hộ");
        }

        double endLat = rescueRequest.getMapPoint().getLocation().getY();
        double endLng = rescueRequest.getMapPoint().getLocation().getX();

        String origin = startLat + "," + startLng;
        String destination = endLat + "," + endLng;

        // Tính toán biên của hộp - lấy thêm khoảng 2km
        double minLat = Math.min(startLat, endLat) - 0.02;
        double maxLat = Math.max(startLat, endLat) + 0.02;
        double minLng = Math.min(startLng, endLng) - 0.02;
        double maxLng = Math.max(startLng, endLng) + 0.02;

        List<HazardSpatialProjection> activeHazards = hazardReportRepository.findActiveHazardsInBoundingBox(minLng, minLat, maxLng, maxLat);

        RouteResponse goongResponse;
        try {
            goongResponse = goongRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/direction")
                            .queryParam("origin", origin)
                            .queryParam("destination", destination)
                            .queryParam("vehicle", finalVehicle)
                            .queryParam("alternatives", "true")
                            .queryParam("api_key", apiKey)
                            .build())
                    .retrieve()
                    .body(RouteResponse.class);
        } catch (Exception e) {
            log.error("Error calling Goong Directions API for vehicle: {}, origin: {}, destination: {}. Message: {}",
                    finalVehicle, origin, destination, e.getMessage(), e);
            throw new BusinessException(ErrorCode.BAD_GATEWAY,  "Không thể tìm tuyến đường từ dịch vụ bản đồ.");
        }

        RouteSafetyEngine.SafetyEvaluationResult evaluation = routeSafetyEngine.evaluateAndSelectSafestRoute(
                goongResponse.routes(),
                activeHazards
        );

        List<RouteResponse.RouteHazardDto> hazardDtos = evaluation.relevantHazards().stream()
                .map(h -> RouteResponse.RouteHazardDto.builder()
                        .id(h.getId())
                        .hazardType(h.getHazardType())
                        .latitude(h.getLatitude())
                        .longitude(h.getLongitude())
                        .build())
                .toList();

        return RouteResponse.builder()
                .routes(evaluation.routes())
                .hazards(hazardDtos)
                .build();
    }
}
