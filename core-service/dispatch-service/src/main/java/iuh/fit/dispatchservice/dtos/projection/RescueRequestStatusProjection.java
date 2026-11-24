package iuh.fit.dispatchservice.dtos.projection;

import iuh.fit.dispatchservice.enums.RequestStatus;

import java.util.UUID;

public interface RescueRequestStatusProjection {
    UUID getId();

    RequestStatus getStatus();
}
