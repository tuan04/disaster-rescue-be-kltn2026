package iuh.fit.dispatchservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.dispatchservice.dtos.request.CreateHazardReportRequest;
import iuh.fit.dispatchservice.dtos.request.CreateSafePointRequest;
import iuh.fit.dispatchservice.dtos.request.CreateWarehouseRequest;
import iuh.fit.dispatchservice.dtos.request.MapPointFilterRequest;
import iuh.fit.dispatchservice.dtos.request.MapPointRequest;
import iuh.fit.dispatchservice.dtos.response.*;
import iuh.fit.dispatchservice.entity.HazardReport;
import iuh.fit.dispatchservice.entity.MapPoint;
import iuh.fit.dispatchservice.entity.SafePoint;
import iuh.fit.dispatchservice.entity.Warehouse;
import iuh.fit.dispatchservice.enums.HazardStatus;
import iuh.fit.dispatchservice.enums.PointType;
import iuh.fit.dispatchservice.repositories.*;
import iuh.fit.dispatchservice.utils.MapPointMapper;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MapPointService {
    private final MapPointRepository mapPointRepository;
    private final MapPointSearchRepository mapPointSearchRepository;
    private final RescueRequestRepository rescueRequestRepository;
    private final SafePointRepository safePointRepository;
    private final WarehouseRepository warehouseRepository;
    private final HazardReportRepository hazardReportRepository;
    private final LocationRepository locationRepository;
    private final S3Service s3Service;
    private final MapPointMapper mapPointMapper;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public List<MapPointRes> getAllMapPoints(MapPointFilterRequest filter) {
        return mapPointSearchRepository.findMapPoints(filter);
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

    }

    private SafePointDetailResponse getSafePointDetail(UUID id) {
        return mapPointMapper.toResDTO(safePointRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
                ));

    }

    private WarehouseDetailResponse getWarehouseDetail(UUID id) {
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
                point.getAddress(),
                point.getCreatedAt(),
                detail
        );
    }

    @Transactional
    public MapPointDetailResponse createWarehouseMapPoint(CreateWarehouseRequest request) {
        Point point = null;
        UUID matchedLocationId = null;

        if (request.getMapPoint() != null) {
            Double lat = request.getMapPoint().getLatitude();
            Double lng = request.getMapPoint().getLongitude();

            if (lat != null && lng != null) {
                point = geometryFactory.createPoint(new Coordinate(lng, lat));
                matchedLocationId = locationRepository.findLocationIdContainingCoordinates(lng, lat).orElse(null);
            }
        }

        MapPoint mapPoint = MapPoint.builder()
                .pointType(PointType.WARE_HOUSE)
                .address(request.getMapPoint() != null ? request.getMapPoint().getAddress() : null)
                .location(point)
                .locationId(matchedLocationId)
                .isVisible(true)
                .build();
        MapPoint savedMapPoint = mapPointRepository.save(mapPoint);

        Warehouse warehouse = Warehouse.builder()
                .id(savedMapPoint.getId())
                .mapPoint(savedMapPoint)
                .name(request.getName())
                .managerPhone(request.getManagerPhone())
                .isActive(true)
                .build();
        Warehouse savedWarehouse = warehouseRepository.save(warehouse);

        return new MapPointDetailResponse(
                savedMapPoint.getId(),
                savedMapPoint.getPointType(),
                mapPointMapper.toLatitude(savedMapPoint.getLocation()),
                mapPointMapper.toLongitude(savedMapPoint.getLocation()),
                savedMapPoint.getAddress(),
                savedMapPoint.getCreatedAt(),
                mapPointMapper.toResDTO(savedWarehouse)
        );
    }

    @Transactional
    public MapPointDetailResponse createSafeMapPoint(CreateSafePointRequest request) {
        Point point = null;
        UUID matchedLocationId = null;

        if (request.getMapPoint() != null) {
            Double lat = request.getMapPoint().getLatitude();
            Double lng = request.getMapPoint().getLongitude();

            if (lat != null && lng != null) {
                point = geometryFactory.createPoint(new Coordinate(lng, lat));
                matchedLocationId = locationRepository.findLocationIdContainingCoordinates(lng, lat).orElse(null);
            }
        }

        MapPoint mapPoint = MapPoint.builder()
                .pointType(PointType.SAFE_ZONE)
                .address(request.getMapPoint() != null ? request.getMapPoint().getAddress() : null)
                .location(point)
                .locationId(matchedLocationId)
                .isVisible(true)
                .build();
        MapPoint savedMapPoint = mapPointRepository.save(mapPoint);

        SafePoint safePoint = SafePoint.builder()
                .id(savedMapPoint.getId())
                .mapPoint(savedMapPoint)
                .name(request.getName())
                .safePointType(request.getSafePointType())
                .contactPhone(request.getContactPhone())
                .isActive(true)
                .build();
        SafePoint savedSafePoint = safePointRepository.save(safePoint);

        return new MapPointDetailResponse(
                savedMapPoint.getId(),
                savedMapPoint.getPointType(),
                mapPointMapper.toLatitude(savedMapPoint.getLocation()),
                mapPointMapper.toLongitude(savedMapPoint.getLocation()),
                savedMapPoint.getAddress(),
                savedMapPoint.getCreatedAt(),
                mapPointMapper.toResDTO(savedSafePoint)
        );
    }

    @Transactional
    public MapPointDetailResponse createHazardReport(CreateHazardReportRequest request, List<MultipartFile> images, UUID reporterId) {
        MapPointRequest mapPointRequest = request.resolveMapPoint();

        Point point = null;
        UUID matchedLocationId = null;

        if (mapPointRequest != null) {
            Double lat = mapPointRequest.getLatitude();
            Double lng = mapPointRequest.getLongitude();

            if (lat != null && lng != null) {
                point = geometryFactory.createPoint(new Coordinate(lng, lat));
                matchedLocationId = locationRepository.findLocationIdContainingCoordinates(lng, lat).orElse(null);
            }
        }

        List<String> allImageUrls = new ArrayList<>();
        List<MultipartFile> filesToUpload = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            filesToUpload.addAll(images);
        }
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (MultipartFile file : request.getImages()) {
                if (!filesToUpload.contains(file)) {
                    filesToUpload.add(file);
                }
            }
        }

        if (!filesToUpload.isEmpty()) {
            List<String> uploadedUrls = s3Service.uploadFiles(filesToUpload);
            allImageUrls.addAll(uploadedUrls);
        }

        MapPoint mapPoint = MapPoint.builder()
                .pointType(PointType.HAZARD)
                .address(mapPointRequest != null ? mapPointRequest.getAddress() : null)
                .location(point)
                .locationId(matchedLocationId)
                .isVisible(true)
                .build();
        MapPoint savedMapPoint = mapPointRepository.save(mapPoint);

        HazardReport hazardReport = HazardReport.builder()
                .id(savedMapPoint.getId())
                .mapPoint(savedMapPoint)
                .reporterId(reporterId)
                .hazardType(request.getHazardType())
                .description(request.getDescription())
                .imageUrls(allImageUrls)
                .status(HazardStatus.ACTIVE)
                .build();
        HazardReport savedHazardReport = hazardReportRepository.save(hazardReport);

        return new MapPointDetailResponse(
                savedMapPoint.getId(),
                savedMapPoint.getPointType(),
                mapPointMapper.toLatitude(savedMapPoint.getLocation()),
                mapPointMapper.toLongitude(savedMapPoint.getLocation()),
                savedMapPoint.getAddress(),
                savedMapPoint.getCreatedAt(),
                mapPointMapper.toResDTO(savedHazardReport)
        );
    }
}
