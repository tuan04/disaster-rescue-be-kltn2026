package iuh.fit.dispatchservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.dispatchservice.dtos.response.*;
import iuh.fit.dispatchservice.entity.MapPoint;
import iuh.fit.dispatchservice.repositories.*;
import iuh.fit.dispatchservice.utils.MapPointMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class MapPointService {
    private final MapPointRepository mapPointRepository;
    private final RescueRequestRepository rescueRequestRepository;
    private final SafePointRepository safePointRepository;
    private final WarehouseRepository warehouseRepository;
    private final HazardReportRepository hazardReportRepository;
    private final MapPointMapper mapPointMapper;

    public List<MapPointRes> getAllMapPoints() {
        return mapPointMapper.toResDTO(mapPointRepository.findMapPoints());
    }

    private RescueDetailResponse getRescueDetail(UUID id) {
        return mapPointMapper.toResDTO(rescueRequestRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
                ));
    }

    private HazardDetailResponse getHazardDetail(UUID id) {
        return mapPointMapper.toResDTO(hazardReportRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
                ));

    }private SafePointDetailResponse getSafePointDetail(UUID id) {
        return mapPointMapper.toResDTO(safePointRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
                ));

    }private WarehouseDetailResponse getWarehouseDetail(UUID id) {
        return mapPointMapper.toResDTO(warehouseRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
                ));
    }

    public MapPointDetailResponse getDetail(UUID id) {
        MapPoint point = mapPointRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
                );

        MapPointDetailResInterface detail = switch (point.getPointType()) {
            case SOS -> getRescueDetail(id);
            case HAZARD -> getHazardDetail(id);
            case SAFE_ZONE -> getSafePointDetail(id);
            case WARE_HOUSE -> getWarehouseDetail(id);
        };

        return new MapPointDetailResponse(
                point.getId(),
                point.getPointType(),
                mapPointMapper.toLatitude(point.getLocation()),
                mapPointMapper.toLongitude(point.getLocation()),
                detail
        );
    }
}
