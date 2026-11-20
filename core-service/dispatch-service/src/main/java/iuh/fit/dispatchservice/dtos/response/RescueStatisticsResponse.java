package iuh.fit.dispatchservice.dtos.response;


import iuh.fit.dispatchservice.enums.StatisticsPeriod;

import java.time.LocalDate;
import java.util.List;

public record RescueStatisticsResponse(
        LocalDate fromDate,
        LocalDate toDate,
        StatisticsPeriod period,
        List<RescueStatisticsBucketResponse> buckets
) {
}