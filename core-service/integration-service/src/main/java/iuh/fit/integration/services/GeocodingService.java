package iuh.fit.integration.services;


import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.integration.dtos.response.NominatimResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class GeocodingService {
    private final RestTemplate restTemplate;

    public String getAddress(double latitude, double longitude){
        String url = String.format("https://nominatim.openstreetmap.org/reverse?format=json&lat=%s&lon=%s", latitude, longitude);
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "DispatchService/1.0 (contact: minhtri084038@gmail.com)");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<NominatimResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                NominatimResponse.class
        );
        if(response.getBody() != null && response.getBody().display_name() != null){
            return response.getBody().display_name();
        }
        else new BusinessException(ErrorCode.FORBIDDEN,"Cannot get address from coordinates: " + latitude + ", " + longitude);
        return null;
    }
}
