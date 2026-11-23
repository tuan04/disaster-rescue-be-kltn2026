package iuh.fit.dispatchservice.utils;

import iuh.fit.dispatchservice.dtos.response.TeamAssignmentHistoryResponse;
import iuh.fit.dispatchservice.entity.Assignment;
import org.locationtech.jts.geom.Point;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Duration;
import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface AssignmentMapper {

    @Mapping(target = "assignmentId", source = "id")
    @Mapping(target = "requestCode", source = "rescueRequest.code")
    @Mapping(target = "emergencyLevel", source = "rescueRequest.emergencyLevel")
    @Mapping(target = "address", source = "rescueRequest.mapPoint.address")
    @Mapping(target = "content", source = "rescueRequest.content")
    @Mapping(target = "reporterPhone", source = "rescueRequest.reporterPhone")
    @Mapping(target = "targetLatitude", source = "rescueRequest.mapPoint.location", qualifiedByName = "toNullableLatitude")
    @Mapping(target = "targetLongitude", source = "rescueRequest.mapPoint.location", qualifiedByName = "toNullableLongitude")
    @Mapping(target = "distanceMeters", source = "distanceMeters", qualifiedByName = "toRoundedDistanceMeters")
    @Mapping(target = "durationMinutes", source = ".", qualifiedByName = "computeDurationMinutes")
    TeamAssignmentHistoryResponse toHistoryResponse(Assignment assignment);

    @Named("toNullableLatitude")
    default Double toNullableLatitude(Point point) {
        return point == null ? null : point.getY();
    }

    @Named("toNullableLongitude")
    default Double toNullableLongitude(Point point) {
        return point == null ? null : point.getX();
    }

    @Named("toRoundedDistanceMeters")
    default Double toRoundedDistanceMeters(Double distanceMeters) {
        return distanceMeters != null ? Math.round(distanceMeters * 10.0) / 10.0 : 0.0;
    }

    @Named("computeDurationMinutes")
    default Long computeDurationMinutes(Assignment assignment) {
        if (assignment == null || assignment.getCompletedAt() == null) {
            return null;
        }
        LocalDateTime start = assignment.getRespondedAt() != null
                ? assignment.getRespondedAt()
                : assignment.getAssignedAt();
        if (start == null) {
            return null;
        }
        long minutes = Duration.between(start, assignment.getCompletedAt()).toMinutes();
        return Math.max(0L, minutes);
    }
}
