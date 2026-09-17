package iuh.fit.dispatchservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.grpc.UserInfoGrpcResponse;
import iuh.fit.dispatchservice.client.UserGrpcClient;
import iuh.fit.dispatchservice.dtos.response.GeoJsonPolygonDto;
import iuh.fit.dispatchservice.dtos.response.LocationPageResponse;
import iuh.fit.dispatchservice.dtos.response.LocationResponse;
import iuh.fit.dispatchservice.entity.Location;
import iuh.fit.dispatchservice.enums.LocationStatus;
import iuh.fit.dispatchservice.repositories.LocationRepository;
import iuh.fit.dispatchservice.repositories.specifications.LocationSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        // 2. Query phân trang Location từ database
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
                            .boundary(GeoJsonPolygonDto.fromPolygon(location.getBoundary()))
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
