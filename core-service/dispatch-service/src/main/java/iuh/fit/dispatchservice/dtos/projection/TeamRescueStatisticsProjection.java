package iuh.fit.dispatchservice.dtos.projection;

public interface TeamRescueStatisticsProjection {
    Long getTotalCompletedCases();
    Long getTotalCanceledCases();
    Long getTotalRejectedCases();
    Double getTotalDistanceMeters();
    Double getAverageDurationMinutes();
}
