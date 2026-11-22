package iuh.fit.integration.dtos.response;

import iuh.fit.integration.enums.WeatherLocationSource;

public record WeatherLocationResponse(
        String name,
        String timezone,
        WeatherLocationSource source
) {}
