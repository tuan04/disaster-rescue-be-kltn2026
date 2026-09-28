package iuh.fit.dispatchservice.services;

import iuh.fit.dispatchservice.dtos.projection.HazardSpatialProjection;
import iuh.fit.dispatchservice.dtos.response.RouteResponse;
import iuh.fit.dispatchservice.enums.HazardType;
import iuh.fit.dispatchservice.utils.PolylineUtils;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class RouteSafetyEngine {

    public static final Map<HazardType, Double> DEFAULT_HAZARD_RADIUS = Map.of(
            HazardType.FALLEN_TREE, 40.0,        
            HazardType.POWER_LINE_DOWN, 60.0,  
            HazardType.FLOOD_DEEP, 120.0,       
            HazardType.LANDSLIDE, 200.0         
    );

    public static final double CORRIDOR_RADIUS_METERS = 500.0;

    public record SafetyEvaluationResult(
            List<RouteResponse.RouteDto> routes,
            List<HazardSpatialProjection> relevantHazards
    ) {
    }

    public SafetyEvaluationResult evaluateAndSelectSafestRoute(
            List<RouteResponse.RouteDto> candidateRoutes,
            List<HazardSpatialProjection> hazards
    ) {
        if (candidateRoutes == null || candidateRoutes.isEmpty()) {
            return new SafetyEvaluationResult(Collections.emptyList(), Collections.emptyList());
        }

        if (hazards == null || hazards.isEmpty()) {
            return new SafetyEvaluationResult(candidateRoutes, Collections.emptyList());
        }

        List<EvaluatedRoute> evaluatedRoutes = new ArrayList<>();

        for (int i = 0; i < candidateRoutes.size(); i++) {
            RouteResponse.RouteDto route = candidateRoutes.get(i);
            String encodedPoints = route.overview_polyline() != null ? route.overview_polyline().points() : null;
            LineString lineString = PolylineUtils.decodeToLineString(encodedPoints);

            int hazardHitCount = 0;
            double minDistanceToHazard = Double.MAX_VALUE;

            if (lineString != null) {
                for (HazardSpatialProjection hazard : hazards) {
                    double dangerRadius = DEFAULT_HAZARD_RADIUS.getOrDefault(hazard.getHazardType(), 50.0);
                    double distance = PolylineUtils.calculateDistanceInMeters(lineString, hazard.getLongitude(), hazard.getLatitude());

                    if (distance < dangerRadius) {
                        hazardHitCount++;
                    }
                    if (distance < minDistanceToHazard) {
                        minDistanceToHazard = distance;
                    }
                }
            }

            long durationSeconds = extractDurationSeconds(route);
            evaluatedRoutes.add(new EvaluatedRoute(route, lineString, hazardHitCount, minDistanceToHazard, durationSeconds, i));
        }

        evaluatedRoutes.sort((r1, r2) -> {
            boolean r1Safe = r1.hazardHits == 0;
            boolean r2Safe = r2.hazardHits == 0;

            if (r1Safe && !r2Safe) return -1;
            if (!r1Safe && r2Safe) return 1;

            if (r1Safe && r2Safe) {
                return Long.compare(r1.durationSeconds, r2.durationSeconds);
            }

            if (r1.hazardHits != r2.hazardHits) {
                return Integer.compare(r1.hazardHits, r2.hazardHits);
            }

            int clearanceComp = Double.compare(r2.minDistanceToHazard, r1.minDistanceToHazard);
            if (clearanceComp != 0) {
                return clearanceComp;
            }
            return Long.compare(r1.durationSeconds, r2.durationSeconds);
        });

        EvaluatedRoute chosenRoute = evaluatedRoutes.get(0);
        log.info("Selected safest route: hits={}, minDistance={}m, duration={}s (original index={})",
                chosenRoute.hazardHits,
                String.format("%.1f", chosenRoute.minDistanceToHazard),
                chosenRoute.durationSeconds,
                chosenRoute.originalIndex);

        List<HazardSpatialProjection> corridorHazards = new ArrayList<>();
        if (chosenRoute.lineString != null) {
            for (HazardSpatialProjection hazard : hazards) {
                double distance = PolylineUtils.calculateDistanceInMeters(chosenRoute.lineString, hazard.getLongitude(), hazard.getLatitude());
                if (distance <= CORRIDOR_RADIUS_METERS) {
                    corridorHazards.add(hazard);
                }
            }
        }

        List<RouteResponse.RouteDto> reorderedRoutes = evaluatedRoutes.stream()
                .map(EvaluatedRoute::route)
                .toList();

        return new SafetyEvaluationResult(reorderedRoutes, corridorHazards);
    }

    private static long extractDurationSeconds(RouteResponse.RouteDto route) {
        if (route != null && route.legs() != null && !route.legs().isEmpty()) {
            RouteResponse.ValueText duration = route.legs().get(0).duration();
            if (duration != null && duration.value() != null) {
                return duration.value().longValue();
            }
        }
        return Long.MAX_VALUE;
    }

    private record EvaluatedRoute(
            RouteResponse.RouteDto route,
            LineString lineString,
            int hazardHits,
            double minDistanceToHazard,
            long durationSeconds,
            int originalIndex
    ) {
    }
}
