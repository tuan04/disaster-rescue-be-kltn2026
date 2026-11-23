package iuh.fit.dispatchservice.dtos.response;

public record TeamRescueStatisticsResponse(
        long totalCompletedCases,
        long totalCanceledCases,
        long totalRejectedCases,
        double totalDistanceMeters,
        Double averageDurationMinutes
) {}
