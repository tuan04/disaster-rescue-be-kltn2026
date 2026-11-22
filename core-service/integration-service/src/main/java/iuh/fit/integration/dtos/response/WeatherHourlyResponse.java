package iuh.fit.integration.dtos.response;

import java.time.Instant;

public record WeatherHourlyResponse(
        Instant time,
        Double temperatureC,
        Integer humidityPercent,
        Integer rainChancePercent,
        WeatherConditionResponse condition
) {}
