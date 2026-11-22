package iuh.fit.integration.dtos.response;

import java.util.List;

public record WeatherResponse(
        WeatherLocationResponse location,
        WeatherCurrentResponse current,
        List<WeatherDailyResponse> days
) {}
