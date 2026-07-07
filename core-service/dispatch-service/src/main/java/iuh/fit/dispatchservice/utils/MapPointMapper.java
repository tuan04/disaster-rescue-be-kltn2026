package iuh.fit.dispatchservice.utils;

import iuh.fit.dispatchservice.dtos.MapPointRes;
import iuh.fit.dispatchservice.entity.MapPoint;
import org.locationtech.jts.geom.Point;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import java.util.List;

@Mapper(componentModel = "spring")
public interface MapPointMapper {

    @Mapping(target = "latitude", source = "location", qualifiedByName = "toLatitude")
    @Mapping(target = "longitude", source = "location", qualifiedByName = "toLongitude")
    MapPointRes toResDTO(MapPoint mapPoint);

    @Mapping(target = "latitude", source = "location", qualifiedByName = "toLatitude")
    @Mapping(target = "longitude", source = "location", qualifiedByName = "toLongitude")
    List<MapPointRes> toResDTO(List<MapPoint> entities);

    @Named("toLatitude")
    default double toLatitude(Point point) {
        return point == null ? 0 : point.getY();
    }

    @Named("toLongitude")
    default double toLongitude(Point point) {
        return point == null ? 0 : point.getX();
    }
}
