package iuh.fit.integration.dtos.response;

import java.time.LocalDate;
import java.util.List;

public record WeatherDailyResponse(
        LocalDate date,
        Double minTemperatureC,
        Double maxTemperatureC,
        Integer rainChancePercent,
        WeatherConditionResponse condition,
        List<WeatherHourlyResponse> hours
) {}
