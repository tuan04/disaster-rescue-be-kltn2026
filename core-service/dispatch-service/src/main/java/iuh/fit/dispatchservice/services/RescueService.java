package iuh.fit.dispatchservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.kafka.dto.AIEvaluationNotificationResponse;
import iuh.fit.common.kafka.dto.SOSRequest;
import iuh.fit.common.kafka.dto.SOSResponse;
import iuh.fit.common.response.PageResponse;
import iuh.fit.dispatchservice.dtos.projection.RescueRequestStatusProjection;
import iuh.fit.dispatchservice.dtos.request.RescueRequestFilterRequest;
import iuh.fit.dispatchservice.dtos.request.UpdateSOSRequest;
import iuh.fit.dispatchservice.dtos.response.RescueRequestListResponse;
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
import iuh.fit.dispatchservice.repositories.specifications.RescueRequestSearchSpecification;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RescueService {
    private static final int MAX_STATUS_BATCH_SIZE = 50;

    private final RescueRequestRepository rescueRequestRepository;
    private final MapPointRepository mapPointRepository;
    private final LocationRepository locationRepository;
    private final SosEventProducer sosEventProducer;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public RescueRequest getRescueRequest(UUID requestId) {
        return rescueRequestRepository.findById(requestId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy yêu cầu cứu hộ"));
    }

    // Đọc dữ liệu và chuyển entity thành DTO trong cùng transaction.
    @Transactional(readOnly = true)
    public PageResponse<RescueRequestListResponse> getRequests(
            RescueRequestFilterRequest request,
            int page, int size
    ) {
        Sort sort = Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        );
        // page=0 là trang đầu.
        Pageable pageable = PageRequest.of(
                page,
                size,
                sort
        );

        var specification = RescueRequestSearchSpecification.filter(request);

        // Repository thực hiện truy vấn dữ liệu và lấy tổng số phù hợp.
        Page<RescueRequest> result = rescueRequestRepository.findAll(
                specification,
                pageable
        );

        Page<RescueRequestListResponse> responsePage = result.map(
                RescueRequestListResponse::from
        );
        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public Map<UUID, RequestStatus> getStatusesByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<UUID> limitedIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .limit(MAX_STATUS_BATCH_SIZE)
                .toList();
        if (limitedIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, RequestStatus> result = new HashMap<>(limitedIds.size());
        for (RescueRequestStatusProjection item : rescueRequestRepository.findStatusesByIdIn(limitedIds)) {
            if (item.getId() != null && item.getStatus() != null) {
                result.put(item.getId(), item.getStatus());
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<RescueRequestListResponse> getMyRequests(UUID reporterId) {
        if (reporterId == null) {
            return List.of();
        }
        return rescueRequestRepository.findByReporterIdOrderByCreatedAtDescIdDesc(reporterId)
                .stream()
                .map(RescueRequestListResponse::from)
                .toList();
    }

    @Transactional
    public SOSResponse createSOSRequest(SOSRequest sosRequest, UUID reporterId) {
        Point sosPoint = geometryFactory.createPoint(
                new Coordinate(sosRequest.getLongitude(), sosRequest.getLatitude())
        );

        Location matchedLocation = locationRepository.findLocationContainingCoordinates(
                        sosRequest.getLongitude(), sosRequest.getLatitude())
                .orElse(null);

        UUID matchedLocationId = matchedLocation != null ? matchedLocation.getId() : null;
        Integer radiusMeters = (matchedLocation != null && matchedLocation.getRadiusMeters() != null)
                ? matchedLocation.getRadiusMeters()
                : 5000;

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
                .eventType("CREATED")
                .id(savedRescueRequest.getId())
                .code(savedRescueRequest.getCode())
                .reporterPhone(savedRescueRequest.getReporterPhone())
                .emergencyLevel(savedRescueRequest.getEmergencyLevel() != null ? savedRescueRequest.getEmergencyLevel().name() : null)
                .content(savedRescueRequest.getContent())
                .status(savedRescueRequest.getStatus() != null ? savedRescueRequest.getStatus().name() : null)
                .source(savedRescueRequest.getSource() != null ? savedRescueRequest.getSource().name() : null)
                .latitude(sosRequest.getLatitude())
                .longitude(sosRequest.getLongitude())
                .radiusMeters(radiusMeters)
                .build();

        sosEventProducer.publishSOSEvent(req);
        return req;
    }

    @Transactional
    public SOSResponse updateSOSRequest(UpdateSOSRequest updateSOSRequest) {
        RescueRequest existingRequest = rescueRequestRepository.findById(updateSOSRequest.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy yêu cầu cứu hộ"));

        // Mobile chỉ gửi các trường được sửa; không xóa dữ liệu/trạng thái còn lại.
        if (updateSOSRequest.getReporterPhone() != null) {
            existingRequest.setReporterPhone(updateSOSRequest.getReporterPhone());
        }
        if (updateSOSRequest.getContent() != null) {
            existingRequest.setContent(updateSOSRequest.getContent());
        }
        if (updateSOSRequest.getStatus() != null) {
            existingRequest.setStatus(updateSOSRequest.getStatus());
        }

        RescueRequest updatedRescueRequest = rescueRequestRepository.save(existingRequest);

        Integer radiusMeters = 5000;
        if (updatedRescueRequest.getMapPoint() != null && updatedRescueRequest.getMapPoint().getLocationId() != null) {
            radiusMeters = locationRepository.findById(updatedRescueRequest.getMapPoint().getLocationId())
                    .map(Location::getRadiusMeters)
                    .orElse(5000);
        }

        SOSResponse response = SOSResponse.builder()
                .eventType("UPDATED")
                .id(updatedRescueRequest.getId())
                .code(updatedRescueRequest.getCode())
                .reporterPhone(updatedRescueRequest.getReporterPhone())
                .emergencyLevel(updatedRescueRequest.getEmergencyLevel() != null ? updatedRescueRequest.getEmergencyLevel().name() : null)
                .content(updatedRescueRequest.getContent())
                .status(updatedRescueRequest.getStatus() != null ? updatedRescueRequest.getStatus().name() : null)
                .source(updatedRescueRequest.getSource() != null ? updatedRescueRequest.getSource().name() : null)
                .latitude(updatedRescueRequest.getMapPoint().getLocation().getY())
                .longitude(updatedRescueRequest.getMapPoint().getLocation().getX())
                .radiusMeters(radiusMeters)
                .build();

        sosEventProducer.publishSOSEvent(response);
        return response;
    }

    @Transactional
    public boolean updateSOSByAI(AIEvaluationNotificationResponse event) {
        RescueRequest existingRequest = rescueRequestRepository.findById(event.requestId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy yêu cầu cứu hộ"));

        existingRequest.setAiEvaluation(event.evaluate());
        existingRequest.setEmergencyLevel(EmergencyLevel.valueOf(event.emergencyLevel()));
        existingRequest.getMapPoint().setAddress(event.address());

        rescueRequestRepository.save(existingRequest);
        return true;
    }
}
