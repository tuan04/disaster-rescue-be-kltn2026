package iuh.fit.dispatchservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.dispatchservice.dtos.response.RouteResponse;
import iuh.fit.dispatchservice.entity.RescueRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Service
@Slf4j
public class RouteService {
    private final RestClient goongRestClient;
    private final RescueService rescueService;
    private final String apiKey;

    public RouteService(
            RestClient.Builder restClientBuilder,
            RescueService rescueService,
            @Value("${goong.base-url:https://rsapi.goong.io}") String baseUrl,
            @Value("${goong.api-key:}") String apiKey) {
        this.goongRestClient = restClientBuilder.baseUrl(baseUrl).build();
        this.rescueService = rescueService;
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

        try {
            return goongRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/direction")
                            .queryParam("origin", origin)
                            .queryParam("destination", destination)
                            .queryParam("vehicle", finalVehicle)
                            .queryParam("api_key", apiKey)
                            .build())
                    .retrieve()
                    .body(RouteResponse.class);
        } catch (Exception e) {
            log.error("Error calling Goong Directions API for vehicle: {}, origin: {}, destination: {}. Message: {}",
                    finalVehicle, origin, destination, e.getMessage(), e);
            throw new BusinessException(ErrorCode.BAD_GATEWAY,
                    "Failed to calculate route from external service: " + e.getMessage());
        }
    }
}
