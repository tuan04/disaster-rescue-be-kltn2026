package iuh.fit.dispatchservice.dtos.response;

import lombok.Builder;

import java.util.List;

@Builder
public record RouteResponse(
        String code,
        List<RouteDto> routes) {
    @Builder
    public record RouteDto(
            Double distance,
            Double duration,
            GeometryDto geometry) {
    }

    @Builder
    public record GeometryDto(
            String type,
            List<List<Double>> coordinates) {
    }
}