package iuh.fit.dispatchservice.dtos.request;

import iuh.fit.dispatchservice.enums.HazardType;
import iuh.fit.dispatchservice.enums.SafePointType;

import java.time.LocalDateTime;
import java.util.UUID;

public record StrategicPointsFilter(
        String pointType, // SAFE_ZONE, HAZARD, WARE_HOUSE
        UUID locationId,
        HazardType hazardType,
        SafePointType safePointType,
        LocalDateTime fromTime,
        LocalDateTime toTime

     ) { }
