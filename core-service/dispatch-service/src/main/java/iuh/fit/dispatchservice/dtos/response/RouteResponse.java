package iuh.fit.dispatchservice.dtos.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.util.List;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record RouteResponse(
        String code,
        List<RouteDto> routes) {

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RouteDto(
            Double distance,
            Double duration,
            GeometryDto geometry,
            List<LegDto> legs) {
    }

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeometryDto(
            String type,
            List<List<Double>> coordinates) {
    }

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LegDto(
            Double distance,
            Double duration,
            String summary,
            List<StepDto> steps) {
    }

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StepDto(
            Double distance,
            Double duration,
            String name,
            ManeuverDto maneuver) {
    }

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ManeuverDto(
            String type,
            String modifier,
            List<Double> location,
            Integer bearing_before,
            Integer bearing_after) {
    }
}