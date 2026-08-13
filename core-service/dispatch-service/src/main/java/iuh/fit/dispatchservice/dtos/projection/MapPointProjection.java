package iuh.fit.dispatchservice.dtos.projection;

import java.util.UUID;

public interface MapPointProjection {
    UUID getId();
    String getPointType();
    Double getLatitude();
    Double getLongitude();
    String getEmergencyLevel();
    String getHazardType();
    String getStatus();
}
