package iuh.fit.dispatchservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.dispatchservice.dtos.request.SOSRequest;
import iuh.fit.dispatchservice.dtos.request.UpdateSOSRequest;
import iuh.fit.dispatchservice.dtos.response.SOSResponse;
import iuh.fit.dispatchservice.entity.Location;
import iuh.fit.dispatchservice.entity.MapPoint;
import iuh.fit.dispatchservice.entity.RescueRequest;
import iuh.fit.dispatchservice.enums.EmergencyLevel;
import iuh.fit.dispatchservice.enums.PointType;
import iuh.fit.dispatchservice.enums.RequestSource;
import iuh.fit.dispatchservice.enums.RequestStatus;
import iuh.fit.dispatchservice.kafka.producer.SosEventProducer;
import iuh.fit.dispatchservice.repositories.LocationRepository;
import iuh.fit.dispatchservice.repositories.MapPointRepository;
import iuh.fit.dispatchservice.repositories.RescueRequestRepository;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class SOSService {
    private final MapPointRepository mapPointRepository;
    private final RescueRequestRepository rescueRequestRepository;
    private final LocationRepository locationRepository;
    private final SosEventProducer sosEventProducer;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public SOSResponse createSOSRequest(SOSRequest sosRequest, UUID reporterId) {
        Point sosPoint = geometryFactory.createPoint(
                new Coordinate(sosRequest.getLongitude(), sosRequest.getLatitude())
        );

        System.out.println("SOS Point: " + sosPoint);

        Location matchedLocation = locationRepository.findLocationContainingCoordinates(
                sosRequest.getLongitude(), sosRequest.getLatitude())
                .orElse(null);

        UUID matchedLocationId = matchedLocation != null ? matchedLocation.getId() : null;
        Integer radiusMeters = (matchedLocation != null && matchedLocation.getRadiusMeters() != null)
                ? matchedLocation.getRadiusMeters()
                : 5000;

        System.out.println("Matched Location ID: " + matchedLocationId);

        MapPoint mapPoint = MapPoint.builder()
                .pointType(PointType.SOS)
                .isVisible(true)
                .location(sosPoint)
                .locationId(matchedLocationId)
                .build();
        MapPoint savedMapPoint = mapPointRepository.save(mapPoint);

        RescueRequest rescueRequest = RescueRequest.builder()
                .reporterPhone(sosRequest.getReporterPhone())
                .content(sosRequest.getContent())
                .mapPoint(savedMapPoint)
                .source(RequestSource.APP)
                .reporterId(reporterId)
                .emergencyLevel(EmergencyLevel.LOW)
                .status(RequestStatus.PENDING)
                .build();
        RescueRequest savedRescueRequest = rescueRequestRepository.save(rescueRequest);

        SOSResponse req = SOSResponse.builder()
                .id(savedRescueRequest.getId())
                .reporterPhone(savedRescueRequest.getReporterPhone())
                .emergencyLevel(savedRescueRequest.getEmergencyLevel())
                .content(savedRescueRequest.getContent())
                .status(savedRescueRequest.getStatus())
                .source(savedRescueRequest.getSource())
                .latitude(sosRequest.getLatitude())
                .longitude(sosRequest.getLongitude())
                .radiusMeters(radiusMeters)
                .build();

        sosEventProducer.publishSOSEvent(req);
        return req;
    }

    public SOSResponse updateSOSRequest(UpdateSOSRequest updateSOSRequest) {
        RescueRequest existingRequest = rescueRequestRepository.findById(updateSOSRequest.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy yêu cầu cứu hộ"));

        existingRequest.setReporterPhone(updateSOSRequest.getReporterPhone());
        existingRequest.setContent(updateSOSRequest.getContent());
        existingRequest.setStatus(updateSOSRequest.getStatus());


        RescueRequest updatedRescueRequest = rescueRequestRepository.save(existingRequest);

        Integer radiusMeters = 5000;
        if (updatedRescueRequest.getMapPoint() != null && updatedRescueRequest.getMapPoint().getLocationId() != null) {
            radiusMeters = locationRepository.findById(updatedRescueRequest.getMapPoint().getLocationId())
                    .map(Location::getRadiusMeters)
                    .orElse(5000);
        }

        SOSResponse response = SOSResponse.builder()
                .id(updatedRescueRequest.getId())
                .reporterPhone(updatedRescueRequest.getReporterPhone())
                .emergencyLevel(updatedRescueRequest.getEmergencyLevel())
                .content(updatedRescueRequest.getContent())
                .status(updatedRescueRequest.getStatus())
                .source(updatedRescueRequest.getSource())
                .latitude(updatedRescueRequest.getMapPoint().getLocation().getY())
                .longitude(updatedRescueRequest.getMapPoint().getLocation().getX())
                .radiusMeters(radiusMeters)
                .build();

        sosEventProducer.publishSOSEvent(response);
        return response;
    }
}
