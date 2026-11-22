package iuh.fit.integration.dtos.response;

import java.time.Instant;

public record WeatherCurrentResponse(
        Double temperatureC,
        Integer humidityPercent,
        WeatherConditionResponse condition,
        Instant updatedAt
) {}
