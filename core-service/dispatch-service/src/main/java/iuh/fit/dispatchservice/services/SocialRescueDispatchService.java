package iuh.fit.dispatchservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.kafka.dto.SOSResponse;
import iuh.fit.common.social.SocialEvents;
import iuh.fit.dispatchservice.entity.DispatchSocialOutbox;
import iuh.fit.dispatchservice.entity.MapPoint;
import iuh.fit.dispatchservice.entity.RescueRequest;
import iuh.fit.dispatchservice.enums.EmergencyLevel;
import iuh.fit.dispatchservice.enums.PointType;
import iuh.fit.dispatchservice.enums.RequestSource;
import iuh.fit.dispatchservice.enums.RequestStatus;
import iuh.fit.dispatchservice.kafka.producer.SocialSosNotificationProducer;
import iuh.fit.dispatchservice.repositories.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

import jakarta.validation.Validator;

import java.time.Instant;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class SocialRescueDispatchService {
    private final SocialRescueReceiptRepository receiptRepository;
    private final MapPointRepository mapPointRepository;
    private final RescueRequestRepository rescueRequestRepository;
    private final LocationRepository locationRepository;
    private final DispatchSocialOutboxRepository outboxRepository;
    private final Validator validator;
    private final ObjectMapper objectMapper;

    private final SocialSosNotificationProducer notificationProducer;

    private final GeometryFactory geometryFactory =
            new GeometryFactory(new PrecisionModel(), 4326);

    // Mọi thao tác ghi trong method cùng thuộc một transaction.
    // Nếu exception thoát ra ngoài, rollback toàn bộ thay đổi.
    @Transactional(rollbackFor = Exception.class)
    public boolean acceptApproved(SocialEvents.Approved event) {
        validateApprovedEvent(event);
        Instant receivedAt = Instant.now();

        // Đăng ký externalId ngay trong transaction hiện tại.
        // PostgreSQL quyết định có trùng dựa trên khóa chính.
        int registered = receiptRepository.TryRegister(
                event.externalId(),
                event.eventId(),
                receivedAt
        );

        if (registered == 0) {
            return false;
        }

        if (registered != 1) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Không thể đăng ký xử lý feed"
            );
        }

        SocialEvents.RescueData data = event.data();

        var point = geometryFactory.createPoint(
                new Coordinate(
                        data.longitude(),
                        data.latitude()
                )
        );

        var matchedLocation =
                locationRepository.findLocationContainingCoordinates(
                        data.longitude(),
                        data.latitude()
                ).orElse(null);

        UUID locationId = matchedLocation == null
                ? null
                : matchedLocation.getId();

        int radiusMeters = 5000;

        MapPoint mapPoint = MapPoint.builder()
                .pointType(PointType.SOS)
                .address(data.address())
                .location(point)
                .locationId(locationId)
                .isVisible(true)
                .build();

        MapPoint savedMapPoint = mapPointRepository.saveAndFlush(mapPoint);


        RescueRequest rescueRequest = RescueRequest.builder()
                .mapPoint(savedMapPoint)
                .reporterPhone(data.reporterPhone())
                .content(data.content())
                .source(RequestSource.SOCIAL)
                .emergencyLevel(
                        EmergencyLevel.valueOf(
                                data.emergencyLevel().name()
                        )
                )
                .status(RequestStatus.PENDING)

                .reporterId(null)
                .build();


        // Flush để rescue_requests có bản ghi trước khi cập nhật khóa ngoại.
        RescueRequest savedRequest = rescueRequestRepository.saveAndFlush(rescueRequest);

        int attached = receiptRepository.AttachRescueRequest(
                event.externalId(),
                event.eventId(),
                savedRequest.getId()
        );

        if (attached != 1) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Không thể liên kết feed với yêu cầu cứu hộ"
            );
        }

        SOSResponse rescue = SOSResponse.builder()
                .id(savedRequest.getId())
                .reporterPhone(savedRequest.getReporterPhone())
                .emergencyLevel(savedRequest.getEmergencyLevel().name())
                .content(savedRequest.getContent())
                .status(savedRequest.getStatus().name())
                .source(savedRequest.getSource().name())
                .latitude(data.latitude())
                .longitude(data.longitude())
                .radiusMeters(radiusMeters)
                .build();

        UUID createdEventId = UUID.randomUUID();

        SocialEvents.Created createdEvent = new SocialEvents.Created(
                createdEventId,
                event.eventId(),
                event.externalId(),
                rescue
        );

        // Nếu serialize lỗi thì receipt và yêu cầu cứu hộ đều rollback.
        String payload = objectMapper.writeValueAsString(createdEvent);
        Instant queuedAt = Instant.now();

        DispatchSocialOutbox outbox = DispatchSocialOutbox.builder()
                .eventId(createdEventId)
                .topic(SocialEvents.CREATED_TOPIC)
                .messageKey(event.externalId())
                .payload(payload)
                .publishState("PENDING")
                .publishNextAt(queuedAt)
                .createdAt(queuedAt)
                .build();

        // Lưu outbox cùng transaction với yêu cầu cứu hộ.
        // Nếu ghi outbox thất bại thì rollback toàn bộ phần vừa tạo.
        outboxRepository.saveAndFlush(outbox);


        // Đăng ký callback sau khi transaction commit thành công.
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {

                        // Truyền trực tiếp object rescue đã tạo trong method.
                        notificationProducer.publish(rescue);
                    }
                }
        );


        return true;
    }


    private void validateApprovedEvent(SocialEvents.Approved event) {

        if (event == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Sự kiện phê duyệt không được để trống"
            );
        }


        var violations = validator.validate(event);

        if (!violations.isEmpty()) {

            String invalidFields = violations.stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .distinct()
                    .sorted()
                    .collect(java.util.stream.Collectors.joining(", "));

            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Sự kiện phê duyệt không hợp lệ tại: " + invalidFields
            );
        }

        // Sau validation, data và hai tọa độ đã được bảo đảm không null.
        SocialEvents.RescueData data = event.data();

        // Chặn các giá trị số đặc biệt như NaN hoặc Infinity.
        if (!Double.isFinite(data.latitude())
                || !Double.isFinite(data.longitude())) {

            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Tọa độ phải là số hữu hạn"
            );
        }
    }

}
