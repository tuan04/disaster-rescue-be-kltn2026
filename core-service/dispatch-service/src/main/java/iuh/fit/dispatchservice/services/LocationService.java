package iuh.fit.dispatchservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.grpc.UserInfoGrpcResponse;
import iuh.fit.dispatchservice.client.UserGrpcClient;
import iuh.fit.dispatchservice.dtos.request.CreateLocationRequest;
import iuh.fit.dispatchservice.dtos.request.UpdateLocationRequest;
import iuh.fit.dispatchservice.dtos.response.GeoJsonPolygon;
import iuh.fit.dispatchservice.dtos.response.LocationPageResponse;
import iuh.fit.dispatchservice.dtos.response.LocationResponse;
import iuh.fit.dispatchservice.entity.Location;
import iuh.fit.dispatchservice.enums.LocationStatus;
import iuh.fit.dispatchservice.repositories.LocationRepository;
import iuh.fit.dispatchservice.repositories.specifications.LocationSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {
    private final LocationRepository locationRepository;
    private final UserGrpcClient userGrpcClient;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional
    public LocationPageResponse createLocation(CreateLocationRequest request) {
        Polygon boundary = null;
        if (request.getBoundary() != null) {
            boundary = request.getBoundary().toPolygon(geometryFactory);
        }

        Location location = Location.builder()
                .name(request.getName().trim())
                .userId(request.getUserId())
                .boundary(boundary)
                .radiusMeters(request.getRadiusMeters() != null ? request.getRadiusMeters() : 5000)
                .status(request.getStatus() != null ? request.getStatus() : LocationStatus.ACTIVE)
                .isActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE)
                .build();

        Location savedLocation = locationRepository.save(location);
        return mapToLocationPageResponse(savedLocation);
    }

    @Transactional
    public LocationPageResponse updateLocation(UUID id, UpdateLocationRequest request) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy khu vực với ID: " + id));

        // Chỉ cho chỉnh sửa các trường: userId, name, radiusMeters, status, isActive
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            location.setName(request.getName().trim());
        }

        if (request.getUserId() != null) {
            location.setUserId(request.getUserId());
        }

        if (request.getRadiusMeters() != null) {
            location.setRadiusMeters(request.getRadiusMeters());
        }

        if (request.getStatus() != null) {
            location.setStatus(request.getStatus());
        }

        if (request.getIsActive() != null) {
            location.setIsActive(request.getIsActive());
        }

        Location updatedLocation = locationRepository.save(location);
        return mapToLocationPageResponse(updatedLocation);
    }

    private LocationPageResponse mapToLocationPageResponse(Location location) {
        String userName = null;
        String userPhone = null;

        if (location.getUserId() != null) {
            Map<UUID, UserInfoGrpcResponse> userMap = userGrpcClient.getUsersByIds(Set.of(location.getUserId()));
            UserInfoGrpcResponse userInfo = userMap.get(location.getUserId());
            if (userInfo != null) {
                userName = userInfo.getFullName();
                userPhone = userInfo.getPhoneNumber();
            }
        }

        return LocationPageResponse.builder()
                .id(location.getId())
                .userId(location.getUserId())
                .name(location.getName())
                .boundary(GeoJsonPolygon.fromPolygon(location.getBoundary()))
                .radiusMeters(location.getRadiusMeters())
                .status(location.getStatus())
                .isActive(location.getIsActive())
                .createdAt(location.getCreatedAt())
                .modifiedAt(location.getModifiedAt())
                .userName(userName)
                .userPhone(userPhone)
                .build();
    }


    public List<LocationResponse> getAllLocations() {
       List<Location> locations =  locationRepository.findByIsActiveTrue().orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN ,"Locations not found"));
       return locations.stream().map(location -> new LocationResponse(location.getId(), location.getName())).toList();
    }


    @Transactional(readOnly = true)
    public Page<LocationPageResponse> getAllLocationsWithPagination(
            Boolean isActive,
            UUID userId,
            LocationStatus status,
            Pageable pageable
    ) {

        Specification<Location> spec = LocationSpecification.filter(isActive, userId, status);
        Page<Location> locationPage = locationRepository.findAll(spec, pageable);

        if (locationPage.isEmpty()) {
            return Page.empty(pageable);
        }

        Set<UUID> userIds = locationPage.getContent().stream()
                .map(Location::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 4. Gọi gRPC 1 lần duy nhất để lấy thông tin các user
        Map<UUID, UserInfoGrpcResponse> userMap = userGrpcClient.getUsersByIds(userIds);

        // 5. Map dữ liệu Location entity và thông tin user từ gRPC vào LocationPageResponse
        List<LocationPageResponse> responses = locationPage.getContent().stream()
                .map(location -> {
                    UserInfoGrpcResponse userInfo = location.getUserId() != null
                            ? userMap.get(location.getUserId())
                            : null;

                    String userName = (userInfo != null) ? userInfo.getFullName() : null;
                    String userPhone = (userInfo != null) ? userInfo.getPhoneNumber() : null;

                    return LocationPageResponse.builder()
                            .id(location.getId())
                            .userId(location.getUserId())
                            .name(location.getName())
                            .boundary(GeoJsonPolygon.fromPolygon(location.getBoundary()))
                            .radiusMeters(location.getRadiusMeters())
                            .status(location.getStatus())
                            .isActive(location.getIsActive())
                            .createdAt(location.getCreatedAt())
                            .modifiedAt(location.getModifiedAt())
                            .userName(userName)
                            .userPhone(userPhone)
                            .build();
                })
                .toList();

        return new PageImpl<>(responses, pageable, locationPage.getTotalElements());
    }
}
