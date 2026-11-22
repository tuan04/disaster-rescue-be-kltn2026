package iuh.fit.integration.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.integration.dtos.response.WeatherConditionResponse;
import iuh.fit.integration.dtos.response.WeatherContextResponse;
import iuh.fit.integration.dtos.response.WeatherCurrentResponse;
import iuh.fit.integration.dtos.response.WeatherDailyResponse;
import iuh.fit.integration.dtos.response.WeatherHourlyResponse;
import iuh.fit.integration.dtos.response.WeatherLocationResponse;
import iuh.fit.integration.dtos.response.WeatherResponse;
import iuh.fit.integration.enums.WeatherLocationSource;
import iuh.fit.integration.redis.WeatherCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class WeatherService {
    private static final String FORECAST_PATH = "/forecast.json";
    private static final String WIDGET_SYSTEM_ERROR_MSG = "Hệ thống dự báo thời tiết tạm thời gián đoạn";

    private final RestTemplate restTemplate;
    private final RestTemplate widgetRestTemplate;
    private final WeatherCache weatherCache;
    private final GeocodingService geocodingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${weather.api.key:}")
    private String apiKey;

    @Value("${weather.api.base-url:https://api.weatherapi.com/v1}")
    private String baseUrl;

    @Value("${weather.api.forecast-days:3}")
    private int forecastDays;

    @Autowired
    public WeatherService(
            RestTemplate restTemplate,
            @Qualifier("weatherWidgetRestTemplate") RestTemplate widgetRestTemplate,
            WeatherCache weatherCache,
            GeocodingService geocodingService
    ) {
        this.restTemplate = restTemplate;
        this.widgetRestTemplate = widgetRestTemplate;
        this.weatherCache = weatherCache;
        this.geocodingService = geocodingService;
    }

    public WeatherContextResponse getWeather(double latitude, double longitude) {
        try {
            JsonNode rootNode = fetchForecastJson(restTemplate, formatCoordinates(latitude, longitude), 1);
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

    public WeatherResponse getWidgetForecast(Double latitude, Double longitude, String clientIp) {
        final String query;
        final WeatherLocationSource source;
        final String cacheKey;
        final int days = Math.max(1, Math.min(forecastDays, 3));

        if (latitude != null || longitude != null) {
            if (!isValidCoordinatePair(latitude, longitude)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "Tọa độ không hợp lệ");
            }
            query = formatCoordinates(latitude, longitude);
            source = WeatherLocationSource.GPS;
            cacheKey = weatherCache.buildGpsKey(roundToGrid(latitude), roundToGrid(longitude), days);
        } else {
            if (clientIp == null || clientIp.isBlank()) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Chưa xác định được khu vực");
            }
            query = clientIp.trim();
            source = WeatherLocationSource.IP;
            cacheKey = weatherCache.buildIpKey(query, days);
        }

        WeatherResponse cached = weatherCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            JsonNode rootNode = fetchForecastJson(widgetRestTemplate, query, days);
            String preferredAreaName = (source == WeatherLocationSource.GPS)
                    ? geocodingService.getShortAreaName(latitude, longitude)
                    : null;
            WeatherResponse response = mapToWeatherResponse(rootNode, source, preferredAreaName);
            weatherCache.put(cacheKey, response);
            return response;
        } catch (RestClientResponseException ex) {
            log.warn("Weather widget upstream error: source={}, status={}", source, ex.getStatusCode().value());
            if (source == WeatherLocationSource.IP && isLocationNotFoundError(ex)) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Chưa xác định được khu vực");
            }
            throw new BusinessException(ErrorCode.BAD_GATEWAY, WIDGET_SYSTEM_ERROR_MSG);
        } catch (Exception ex) {
            log.warn("Weather widget error: source={}, errorType={}", source, ex.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.BAD_GATEWAY, WIDGET_SYSTEM_ERROR_MSG);
        }
    }

    public WeatherResponse mapToWeatherResponse(
            JsonNode rootNode,
            WeatherLocationSource source,
            String preferredAreaName
    ) {
        JsonNode locationNode = rootNode.path("location");
        JsonNode currentNode = rootNode.path("current");
        JsonNode forecastDaysNode = rootNode.path("forecast").path("forecastday");

        String rawLocationName = textOrNull(locationNode.path("name"));
        String locationName = (preferredAreaName != null && !preferredAreaName.isBlank())
                ? preferredAreaName.trim()
                : rawLocationName;
        String timezone = textOrNull(locationNode.path("tz_id"));

        List<WeatherDailyResponse> days = new ArrayList<>();
        for (JsonNode item : forecastDaysNode) {
            String dateStr = textOrNull(item.path("date"));
            if (dateStr == null) {
                continue;
            }
            LocalDate date;
            try {
                date = LocalDate.parse(dateStr);
            } catch (Exception ignored) {
                continue;
            }
            JsonNode dayNode = item.path("day");
            List<WeatherHourlyResponse> hours = mapHourlyForecast(item.path("hour"));
            days.add(new WeatherDailyResponse(
                    date,
                    doubleOrNull(dayNode.path("mintemp_c")),
                    doubleOrNull(dayNode.path("maxtemp_c")),
                    integerOrNull(dayNode.path("daily_chance_of_rain")),
                    mapCondition(dayNode.path("condition")),
                    hours
            ));
        }

        if (locationName == null || !currentNode.isObject() || days.isEmpty()) {
            throw new IllegalStateException("Weather payload missing required location, current, or daily data");
        }

        days.sort(Comparator.comparing(WeatherDailyResponse::date));

        Long epochSeconds = longOrNull(currentNode.path("last_updated_epoch"));
        Instant updatedAt = epochSeconds != null ? Instant.ofEpochSecond(epochSeconds) : null;
        Integer currentRainChance = integerOrNull(currentNode.path("chance_of_rain"));
        WeatherCurrentResponse current = new WeatherCurrentResponse(
                doubleOrNull(currentNode.path("temp_c")),
                doubleOrNull(currentNode.path("feelslike_c")),
                integerOrNull(currentNode.path("humidity")),
                currentRainChance,
                mapCondition(currentNode.path("condition")),
                updatedAt
        );

        return new WeatherResponse(
                new WeatherLocationResponse(locationName, timezone, source),
                current,
                List.copyOf(days)
        );
    }

    private List<WeatherHourlyResponse> mapHourlyForecast(JsonNode hoursNode) {
        if (hoursNode == null || !hoursNode.isArray()) {
            return List.of();
        }
        List<WeatherHourlyResponse> hours = new ArrayList<>();
        for (JsonNode hourNode : hoursNode) {
            Long timeEpoch = longOrNull(hourNode.path("time_epoch"));
            if (timeEpoch == null) {
                continue;
            }
            hours.add(new WeatherHourlyResponse(
                    Instant.ofEpochSecond(timeEpoch),
                    doubleOrNull(hourNode.path("temp_c")),
                    integerOrNull(hourNode.path("humidity")),
                    integerOrNull(hourNode.path("chance_of_rain")),
                    mapCondition(hourNode.path("condition"))
            ));
        }
        hours.sort(Comparator.comparing(WeatherHourlyResponse::time));
        return List.copyOf(hours);
    }

    private JsonNode fetchForecastJson(RestTemplate client, String query, int days) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Weather API key is not configured");
        }
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path(FORECAST_PATH)
                .queryParam("key", apiKey)
                .queryParam("q", query)
                .queryParam("days", days)
                .build()
                .encode()
                .toUri();

        String jsonResponse = client.getForObject(uri, String.class);
        if (jsonResponse == null || jsonResponse.isBlank()) {
            throw new IllegalStateException("Weather API returned an empty response");
        }
        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        if (rootNode == null || !rootNode.isObject()) {
            throw new IllegalStateException("Weather API response is not a JSON object");
        }
        return rootNode;
    }

    private boolean isValidCoordinatePair(Double latitude, Double longitude) {
        return latitude != null && longitude != null
                && Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90.0 && latitude <= 90.0
                && longitude >= -180.0 && longitude <= 180.0;
    }

    private boolean isLocationNotFoundError(RestClientResponseException ex) {
        return ex.getStatusCode().value() == 400
                && ex.getResponseBodyAsString().contains("\"code\":1006");
    }

    double roundToGrid(double value) {
        return Math.round(value * 20.0) / 20.0;
    }

    String formatCoordinates(double latitude, double longitude) {
        return BigDecimal.valueOf(latitude).toPlainString() + "," + BigDecimal.valueOf(longitude).toPlainString();
    }

    private WeatherConditionResponse mapCondition(JsonNode conditionNode) {
        return new WeatherConditionResponse(
                textOrNull(conditionNode.path("text")),
                normalizeIconUrl(textOrNull(conditionNode.path("icon")))
        );
    }

    private String normalizeIconUrl(String rawIcon) {
        if (rawIcon == null) {
            return null;
        }
        if (rawIcon.startsWith("//")) {
            return "https:" + rawIcon;
        }
        if (rawIcon.startsWith("http://")) {
            return "https://" + rawIcon.substring("http://".length());
        }
        return rawIcon;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText("").trim();
        return text.isEmpty() ? null : text;
    }

    private Double doubleOrNull(JsonNode node) {
        return (node != null && node.isNumber()) ? node.asDouble() : null;
    }

    private Integer integerOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asInt();
        }
        if (node.isTextual()) {
            try {
                return Integer.parseInt(node.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Long longOrNull(JsonNode node) {
        return (node != null && node.isNumber()) ? node.asLong() : null;
    }
}
