package iuh.fit.dispatchservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.dispatchservice.dtos.response.RouteResponse;
import iuh.fit.dispatchservice.entity.RescueRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteService {
    private final RestClient restClient;
    private final RescueService rescueService;

    public RouteResponse getRoute(Double startLat, Double startLng, UUID requestId, String profile) {
        if (profile == null || profile.trim().isEmpty()) {
            profile = "driving";
        }

        RescueRequest rescueRequest = rescueService.getRescueRequest(requestId);
        if (rescueRequest.getMapPoint() == null || rescueRequest.getMapPoint().getLocation() == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy vị trí yêu cầu cứu hộ");
        }

        double endLat = rescueRequest.getMapPoint().getLocation().getY();
        double endLng = rescueRequest.getMapPoint().getLocation().getX();

        String coordinates = startLng + "," + startLat + ";" + endLng + "," + endLat;
        String finalProfile = profile;

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/route/v1/{profile}/{coordinates}")
                            .queryParam("overview", "full")
                            .queryParam("geometries", "geojson")
                            .queryParam("steps", "true")
                            .build(finalProfile, coordinates))
                    .retrieve()
                    .body(RouteResponse.class);
        } catch (Exception e) {
            log.error("Error calling external OSRM API for profile: {}, coordinates: {}. Message: {}",
                    finalProfile, coordinates, e.getMessage(), e);
            throw new BusinessException(ErrorCode.BAD_GATEWAY,
                    "Failed to calculate route from external service: " + e.getMessage());
        }
    }
}
