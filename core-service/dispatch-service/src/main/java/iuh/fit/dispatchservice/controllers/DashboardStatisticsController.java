package iuh.fit.dispatchservice.controllers;


import iuh.fit.common.response.ApiResponse;
import iuh.fit.dispatchservice.dtos.response.DashboardCountResponse;
import iuh.fit.dispatchservice.dtos.response.RescueStatisticsResponse;
import iuh.fit.dispatchservice.enums.StatisticsPeriod;
import iuh.fit.dispatchservice.services.DashboardStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sos-requests/statistics")
public class DashboardStatisticsController {
    private final DashboardStatisticsService dashboardStatisticsService;


    // Ô 1: Chờ tiếp nhận.
    @GetMapping("/waiting")
    public ResponseEntity<ApiResponse<DashboardCountResponse>> countWaiting() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        dashboardStatisticsService.countWaitingRequests()
                )
        );
    }


    @GetMapping("/processing")
    public ResponseEntity<ApiResponse<DashboardCountResponse>> countProcessing() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        dashboardStatisticsService.countProcessingRequests()
                )
        );
    }


    @GetMapping("/completed")
    public ResponseEntity<ApiResponse<DashboardCountResponse>> countCompleted(
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        LocalDate selectedDate = date != null
                ? date
                : LocalDate.now();

        return ResponseEntity.ok(
                ApiResponse.success(
                        dashboardStatisticsService
                                .countCompletedRequestsCreatedOn(selectedDate)
                )
        );
    }

    @GetMapping("/chart")
    public ResponseEntity<ApiResponse<RescueStatisticsResponse>>
    getRescueStatistics(
            @RequestParam(name = "fromDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(name = "toDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(name = "period", defaultValue = "DAY")
            StatisticsPeriod period
    ) {
        RescueStatisticsResponse response =
                dashboardStatisticsService.getRescueStatistics(
                        fromDate,
                        toDate,
                        period
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        response,
                        "Lấy thống kê ca cứu hộ thành công"
                )
        );
    }


}
