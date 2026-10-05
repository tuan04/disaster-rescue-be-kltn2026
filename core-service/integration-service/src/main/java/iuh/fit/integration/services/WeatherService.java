package iuh.fit.integration.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.integration.dtos.response.WeatherContextResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class WeatherService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${weather.api.key}")
    private String apiKey;

    public WeatherContextResponse getWeather(double latitude, double longitude) {
        String url = "https://api.weatherapi.com/v1/forecast.json?key={key}&q={latitude},{longitude}&days=1";
        try {
            String jsonResponse = restTemplate.getForObject(
                    url, String.class, apiKey, latitude, longitude);
            if (jsonResponse == null || jsonResponse.isBlank()) {
                throw new IllegalStateException("Weather API returned an empty response");
            }
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode currentNode = rootNode.path("current");
            JsonNode dayNode = rootNode.path("forecast").path("forecastday").path(0).path("day");
            if (!currentNode.isObject() || !dayNode.isObject()) {
                throw new IllegalStateException("Weather API response is missing current or forecast day data");
            }

            return WeatherContextResponse.builder()
                    .condition(currentNode.path("condition").path("text").asText("Không rõ"))
                    .temperature(currentNode.path("temp_c").asDouble(0.0))
                    .gustKph(currentNode.path("gust_kph").asDouble(0.0))
                    .precipMm(currentNode.path("precip_mm").asDouble(0.0))
                    .visibilityKm(currentNode.path("vis_km").asDouble(10.0))
                    .isDay(currentNode.path("is_day").asInt(1))
                    .pressureMb(currentNode.path("pressure_mb").asDouble(1013.0))
                    .totalPrecipMm(dayNode.path("totalprecip_mm").asDouble(0.0))
                    .build();
        } catch (Exception e) {
            BusinessException exception = new BusinessException(ErrorCode.BAD_GATEWAY,
                    "Cannot get weather data from coordinates: " + latitude + ", " + longitude);
            exception.initCause(e);
            throw exception;
        }
    }
}
