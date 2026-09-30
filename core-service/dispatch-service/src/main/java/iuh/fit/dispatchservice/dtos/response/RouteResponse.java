package iuh.fit.dispatchservice.dtos.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import iuh.fit.dispatchservice.enums.HazardType;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record RouteResponse(
        List<RouteDto> routes,
        List<RouteHazardDto> hazards) {

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RouteHazardDto(
            UUID id,
            HazardType hazardType,
            Double latitude,
            Double longitude) {
    }


    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RouteDto(
            OverviewPolyline overview_polyline,
            List<LegDto> legs,
            String summary) {
    }

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OverviewPolyline(
            String points) {
    }

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LegDto(
            ValueText distance,
            ValueText duration,
            String start_address,
            String end_address,
            LocationDto start_location,
            LocationDto end_location,
            List<StepDto> steps) {
    }

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StepDto(
            ValueText distance,
            ValueText duration,
            LocationDto start_location,
            LocationDto end_location,
            String html_instructions,
            String maneuver,
            OverviewPolyline polyline,
            String travel_mode) {
    }

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LocationDto(
            Double lat,
            Double lng) {
    }

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ValueText(
            Double value,
            String text) {
    }
}