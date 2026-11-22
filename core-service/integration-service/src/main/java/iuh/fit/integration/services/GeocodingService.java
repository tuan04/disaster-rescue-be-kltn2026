package iuh.fit.integration.services;

import iuh.fit.integration.dtos.response.GoongGeocodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GeocodingService {
    private final RestClient goongRestClient;
    private final String apiKey;

    public GeocodingService(
            RestClient.Builder restClientBuilder,
            @Value("${goong.base-url:https://rsapi.goong.io}") String baseUrl,
            @Value("${goong.api-key:}") String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(4000);
        this.goongRestClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
        this.apiKey = apiKey;
    }

    public String getAddress(double latitude, double longitude) {
        try {
            GoongGeocodeResponse response = fetchGeocode(latitude, longitude);
            if (response != null && response.results() != null && !response.results().isEmpty()) {
                return response.results().get(0).formatted_address();
            }
        } catch (Exception e) {
            log.error("Error calling Goong Geocoding API for coordinates: ({}, {}). Message: {}",
                    latitude, longitude, e.getMessage(), e);
        }
        return "Tọa độ (" + latitude + ", " + longitude + ")";
    }

    /**
     * Trích xuất tên khu vực ngắn gọn tiếng Việt (ưu tiên Xã/Phường + Quận/Huyện hoặc Tỉnh/TP)
     * từ Goong V2; trả về null nếu lỗi hoặc không đủ dữ liệu để caller tự fallback.
     */
    public String getShortAreaName(double latitude, double longitude) {
        try {
            GoongGeocodeResponse response = fetchGeocode(latitude, longitude);
            if (response == null || response.results() == null || response.results().isEmpty()) {
                return null;
            }
            for (GoongGeocodeResponse.Result result : response.results()) {
                if (result == null) {
                    continue;
                }
                String area = extractShortAreaName(result);
                if (area != null) {
                    return area;
                }
            }
        } catch (Exception e) {
            log.warn("Goong reverse geocoding failed for ({}, {}): {}", latitude, longitude, e.getMessage());
        }
        return null;
    }

    private GoongGeocodeResponse fetchGeocode(double latitude, double longitude) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        return goongRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/geocode/street")
                        .queryParam("latlng", latitude + "," + longitude)
                        .queryParam("has_deprecated_administrative_unit", "true")
                        .queryParam("api_key", apiKey)
                        .build())
                .retrieve()
                .body(GoongGeocodeResponse.class);
    }

    private String extractShortAreaName(GoongGeocodeResponse.Result result) {
        String area = formatCompound(result.compound());
        if (area != null) {
            return area;
        }
        area = formatCompound(result.deprecated_compound());
        if (area != null) {
            return area;
        }
        String address = trimToNull(result.address());
        if (address != null) {
            return address;
        }
        return trimToNull(result.formatted_address());
    }

    private String formatCompound(GoongGeocodeResponse.Compound compound) {
        if (compound == null) {
            return null;
        }
        String commune = trimToNull(compound.commune());
        String district = trimToNull(compound.district());
        String province = trimToNull(compound.province());

        if (commune != null && district != null) {
            return commune + ", " + district;
        }
        if (commune != null && province != null) {
            return commune + ", " + province;
        }
        List<String> parts = new ArrayList<>(3);
        if (commune != null) parts.add(commune);
        if (district != null) parts.add(district);
        if (province != null) parts.add(province);
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
