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
        String url = String.format(
                "https://api.weatherapi.com/v1/history.json?key=%s&q=%s,%s&dt=2024-09-07&hour=13",
                apiKey, latitude, longitude);
        try{
            String jsonResponse = restTemplate.getForObject(url, String.class);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode forecastDayNode = rootNode.path("forecast").path("forecastday").get(0);
            JsonNode dayNode = forecastDayNode.path("day");
            JsonNode hourNode = forecastDayNode.path("hour").get(0);

            return WeatherContextResponse.builder()
                    .condition(hourNode.path("condition").path("text").asText("Không rõ"))
                    .temperature(hourNode.path("temp_c").asDouble(0.0))
                    .gustKph(hourNode.path("gust_kph").asDouble(0.0))
                    .precipMm(hourNode.path("precip_mm").asDouble(0.0))
                    .visibilityKm(hourNode.path("vis_km").asDouble(10.0))
                    .isDay(hourNode.path("is_day").asInt(1))
                    .pressureMb(hourNode.path("pressure_mb").asDouble(1013.0))
                    .totalPrecipMm(dayNode.path("totalprecip_mm").asDouble(0.0))
                    .build();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot get weather data from coordinates: " + latitude + ", " + longitude);
        }
    }



}
