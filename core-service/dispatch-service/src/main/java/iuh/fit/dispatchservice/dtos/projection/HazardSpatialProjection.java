package iuh.fit.dispatchservice.dtos.projection;

import iuh.fit.dispatchservice.enums.HazardType;

import java.util.UUID;

public interface HazardSpatialProjection {
    UUID getId();
    HazardType getHazardType();
    Double getLatitude();
    Double getLongitude();
}
