package iuh.fit.integration.services;

import iuh.fit.integration.dtos.response.GoongGeocodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class GeocodingService {
    private final RestClient goongRestClient;
    private final String apiKey;

    public GeocodingService(
            RestClient.Builder restClientBuilder,
            @Value("${goong.base-url:https://rsapi.goong.io}") String baseUrl,
            @Value("${goong.api-key:}") String apiKey) {
        this.goongRestClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public String getAddress(double latitude, double longitude) {
        try {
            GoongGeocodeResponse response = goongRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/geocode/street")
                            .queryParam("latlng", latitude + "," + longitude)
                            .queryParam("has_deprecated_administrative_unit", "true")
                            .queryParam("api_key", apiKey)
                            .build())
                    .retrieve()
                    .body(GoongGeocodeResponse.class);

            if (response != null && response.results() != null && !response.results().isEmpty()) {
                return response.results().get(0).formatted_address();

            }
        } catch (Exception e) {
            log.error("Error calling Goong Geocoding API for coordinates: ({}, {}). Message: {}",
                    latitude, longitude, e.getMessage(), e);
        }
        return "Tọa độ (" + latitude + ", " + longitude + ")";
    }
}
