package iuh.fit.integration.dtos.response;

import java.time.Instant;

public record WeatherCurrentResponse(
        Double temperatureC,
        Double feelsLikeC,
        Integer humidityPercent,
        Integer rainChancePercent,
        WeatherConditionResponse condition,
        Instant updatedAt
) {}
