package iuh.fit.dispatchservice.services;


import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.dispatchservice.dtos.projection.RescueStatisticsProjection;
import iuh.fit.dispatchservice.dtos.response.DashboardCountResponse;
import iuh.fit.dispatchservice.dtos.response.RescueStatisticsBucketResponse;
import iuh.fit.dispatchservice.dtos.response.RescueStatisticsResponse;
import iuh.fit.dispatchservice.enums.AssignmentStatus;
import iuh.fit.dispatchservice.enums.RequestStatus;
import iuh.fit.dispatchservice.enums.StatisticsPeriod;
import iuh.fit.dispatchservice.repositories.RescueRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardStatisticsService {
    private static final List<AssignmentStatus> ACTIVE_ASSIGNMENT_STATUSES =
            List.of(
                    AssignmentStatus.ASSIGNED,
                    AssignmentStatus.ACCEPTED
            );

    private static final List<RequestStatus> PROCESSING_REQUEST_STATUSES =
            List.of(
                    RequestStatus.PENDING,
                    RequestStatus.ACCEPTED
            );

    private final RescueRequestRepository rescueRequestRepository;


    public DashboardCountResponse countWaitingRequests() {
        long count = rescueRequestRepository.countWaitingRequests(
                RequestStatus.PENDING,
                ACTIVE_ASSIGNMENT_STATUSES
        );

        return new DashboardCountResponse(count);
    }

    public DashboardCountResponse countProcessingRequests() {
        long count = rescueRequestRepository.countProcessingRequests(
                PROCESSING_REQUEST_STATUSES,
                ACTIVE_ASSIGNMENT_STATUSES
        );

        return new DashboardCountResponse(count);
    }

    public DashboardCountResponse countCompletedRequestsCreatedOn(
            LocalDate date
    ) {
        long count = rescueRequestRepository
                .countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        RequestStatus.COMPLETED,
                        date.atStartOfDay(),
                        date.plusDays(1).atStartOfDay()
                );

        return new DashboardCountResponse(count);
    }



    public RescueStatisticsResponse getRescueStatistics(
            LocalDate fromDate,
            LocalDate toDate,
            StatisticsPeriod period
    ) {
        if (fromDate == null || toDate == null || period == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Thiếu ngày bắt đầu, ngày kết thúc hoặc kỳ thống kê"
            );
        }

        if (fromDate.isAfter(toDate)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Ngày bắt đầu không được sau ngày kết thúc"
            );
        }

        // Giới hạn miền ngày để tránh tràn khi cộng ngày/tháng/năm.
        if (fromDate.getYear() < 1 || toDate.getYear() > 9998) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Năm thống kê phải nằm trong khoảng 1 đến 9998"
            );
        }

        LocalDate firstBucket = getPeriodStart(fromDate, period);
        LocalDate lastBucket = getPeriodStart(toDate, period);

        Map<LocalDate, RescueStatisticsBucketResponse> buckets =
                new LinkedHashMap<>();

        // Tạo sẵn các kỳ với số lượng 0.
        LocalDate cursor = firstBucket;

        while (!cursor.isAfter(lastBucket)) {
            if (buckets.size() >= 400) {
                throw new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "Tối đa 400 kỳ mỗi lần. Hãy thu hẹp khoảng ngày "
                                + "hoặc chọn kỳ tuần, tháng, năm"
                );
            }

            buckets.put(
                    cursor,
                    new RescueStatisticsBucketResponse(cursor, 0L, 0L, 0L)
            );

            cursor = nextPeriodStart(cursor, period);
        }

        String periodUnit = switch (period) {
            case DAY -> "day";
            case WEEK -> "week";
            case MONTH -> "month";
            case YEAR -> "year";
        };

        List<RescueStatisticsProjection> rows =
                rescueRequestRepository.aggregateRescueStatistics(
                        periodUnit,
                        fromDate.atStartOfDay(),
                        toDate.plusDays(1).atStartOfDay()
                );

        // Thay số 0 bằng số liệu thực tế của những kỳ có dữ liệu.
        for (RescueStatisticsProjection row : rows) {
            LocalDate periodStart = row.getPeriodStart();

            buckets.put(
                    periodStart,
                    new RescueStatisticsBucketResponse(
                            periodStart,
                            row.getCompleted(),
                            row.getSafe(),
                            row.getPending()
                    )
            );
        }

        return new RescueStatisticsResponse(
                fromDate,
                toDate,
                period,
                List.copyOf(buckets.values())
        );
    }

    private LocalDate getPeriodStart(
            LocalDate date,
            StatisticsPeriod period
    ) {
        return switch (period) {
            case DAY -> date;
            case WEEK -> date.with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
            );
            case MONTH -> date.withDayOfMonth(1);
            case YEAR -> date.withDayOfYear(1);
        };
    }

    private LocalDate nextPeriodStart(
            LocalDate date,
            StatisticsPeriod period
    ) {
        return switch (period) {
            case DAY -> date.plusDays(1);
            case WEEK -> date.plusWeeks(1);
            case MONTH -> date.plusMonths(1);
            case YEAR -> date.plusYears(1);
        };
    }

}
