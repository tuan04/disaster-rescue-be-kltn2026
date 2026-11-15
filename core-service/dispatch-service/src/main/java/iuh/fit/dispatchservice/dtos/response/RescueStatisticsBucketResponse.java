package iuh.fit.dispatchservice.dtos.response;

import java.time.LocalDate;

public record RescueStatisticsBucketResponse(
        LocalDate periodStart,
        long completed,
        long safe,
        long pending
) {
}