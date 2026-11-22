package iuh.fit.integration.controller;

import iuh.fit.common.response.ApiResponse;
import iuh.fit.integration.dtos.response.WeatherResponse;
import iuh.fit.integration.services.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping
    public ResponseEntity<ApiResponse<WeatherResponse>> getWeatherForecast(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestHeader(value = "X-Client-Ip", required = false) String clientIp
    ) {
        WeatherResponse response = weatherService.getWidgetForecast(latitude, longitude, clientIp);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(ApiResponse.success(response, "Lấy dự báo thời tiết thành công"));
    }
}
