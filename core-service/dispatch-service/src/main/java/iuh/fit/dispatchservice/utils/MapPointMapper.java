package iuh.fit.dispatchservice.utils;

import iuh.fit.dispatchservice.dtos.response.*;
import iuh.fit.dispatchservice.dtos.projection.MapPointProjection;
import iuh.fit.dispatchservice.entity.HazardReport;
import iuh.fit.dispatchservice.entity.RescueRequest;
import iuh.fit.dispatchservice.entity.SafePoint;
import iuh.fit.dispatchservice.entity.Warehouse;
import org.locationtech.jts.geom.Point;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MapPointMapper {

    @Mapping(target = "priority", source = "emergencyLevel")
    @Mapping(target = "subType", source = "hazardType")
    MapPointRes toResDTO(MapPointProjection projection);

    List<MapPointRes> toResDTO(List<MapPointProjection> projections);

    @Named("toLatitude")
    default double toLatitude(Point point) {
        return point == null ? 0 : point.getY();
    }

    @Named("toLongitude")
    default double toLongitude(Point point) {
        return point == null ? 0 : point.getX();
    }

    HazardDetailResponse toResDTO(HazardReport hazard);

    RescueDetailResponse toResDTO(RescueRequest rescue);

    SafePointDetailResponse toResDTO(SafePoint safePoint);

    WarehouseDetailResponse toResDTO(Warehouse warehouse);
}
