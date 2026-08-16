package iuh.fit.dispatchservice.dtos.request;

import iuh.fit.dispatchservice.enums.*;

import java.time.LocalDateTime;
import java.util.List;

public record MapPointFilterRequest(
        List<PointType> pointTypes,
        List<RequestStatus> rescueStatuses,
        List<EmergencyLevel> emergencyLevels,
        List<HazardStatus> hazardStatuses,
        List<HazardType> hazardTypes,
        List<SafePointType> safePointTypes,
        LocalDateTime fromTime,
        LocalDateTime toTime

) { }
