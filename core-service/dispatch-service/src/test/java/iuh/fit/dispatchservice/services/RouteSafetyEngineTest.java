package iuh.fit.dispatchservice.services;

import iuh.fit.dispatchservice.dtos.projection.HazardSpatialProjection;
import iuh.fit.dispatchservice.dtos.response.RouteResponse;
import iuh.fit.dispatchservice.enums.HazardType;
import iuh.fit.dispatchservice.utils.PolylineUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RouteSafetyEngineTest {

    private final RouteSafetyEngine engine = new RouteSafetyEngine();

    @Test
    @DisplayName("PolylineUtils: Decode known Google encoded polyline string")
    void testPolylineDecode() {
        // Encoded polyline representing: (38.5, -120.2), (40.7, -120.95), (43.252, -126.453)
        String sampleEncoded = "_p~iF~ps|U_ulLnnqC_mqNvxq`@";
        List<Coordinate> coords = PolylineUtils.decode(sampleEncoded);

        assertEquals(3, coords.size());
        // JTS Coordinate: x=lng, y=lat
        assertEquals(-120.2, coords.get(0).x, 1e-4);
        assertEquals(38.5, coords.get(0).y, 1e-4);

        assertEquals(-120.95, coords.get(1).x, 1e-4);
        assertEquals(40.7, coords.get(1).y, 1e-4);

        assertEquals(-126.453, coords.get(2).x, 1e-4);
        assertEquals(43.252, coords.get(2).y, 1e-4);

        LineString line = PolylineUtils.decodeToLineString(sampleEncoded);
        assertNotNull(line);
        assertEquals(3, line.getNumPoints());
    }

    @Test
    @DisplayName("Distance calculation: Point on line should be ~0m, point 100m away should be ~100m")
    void testCalculateDistanceInMeters() {
        // Line from (106.6600, 10.7600) to (106.6700, 10.7600) - horizontal line at lat 10.76
        // Encode manually or use coordinates
        // At lat 10.76, 0.0009 degrees latitude is ~ 100 meters
        String lineEncoded = createStraightPolyline(10.7600, 106.6600, 10.7600, 106.6700);
        LineString line = PolylineUtils.decodeToLineString(lineEncoded);

        // Point exactly on the line
        double distOnLine = PolylineUtils.calculateDistanceInMeters(line, 106.6650, 10.7600);
        assertEquals(0.0, distOnLine, 1.0);

        // Point 0.0009 degrees north (~100.1 meters)
        double distAway = PolylineUtils.calculateDistanceInMeters(line, 106.6650, 10.7609);
        assertEquals(100.1, distAway, 2.0);
    }

    @Test
    @DisplayName("Hazard Avoidance: Should reroute away from flood zone even if detour takes longer")
    void testAvoidFloodZone() {
        // Route 1 (Direct): Goes straight through (106.6650, 10.7600). Duration: 300s (5 mins)
        String route1Polyline = createStraightPolyline(10.7600, 106.6600, 10.7600, 106.6700);
        RouteResponse.RouteDto route1 = createRouteDto(route1Polyline, 300, 1000, "Direct Path");

        // Route 2 (Detour): Bypasses north via lat 10.7630 (~330m away). Duration: 450s (7.5 mins)
        // Waypoints: (10.7600, 106.6600) -> (10.7630, 106.6650) -> (10.7600, 106.6700)
        String route2Polyline = createDetourPolyline();
        RouteResponse.RouteDto route2 = createRouteDto(route2Polyline, 450, 1500, "Detour Path");

        // Hazard: Deep Flood (FLOOD_DEEP, danger radius = 120m) right on the direct route at (10.7600, 106.6650)
        HazardSpatialProjection floodHazard = createHazardMock(
                UUID.randomUUID(), HazardType.FLOOD_DEEP, 10.7600, 106.6650
        );

        // Test with flood present: Route 2 (Detour) must be selected as routes[0]!
        RouteSafetyEngine.SafetyEvaluationResult resultWithFlood = engine.evaluateAndSelectSafestRoute(
                List.of(route1, route2),
                List.of(floodHazard)
        );

        assertNotNull(resultWithFlood);
        assertEquals(2, resultWithFlood.routes().size());
        assertEquals("Detour Path", resultWithFlood.routes().get(0).summary(),
                "Route 2 (Detour) should be at index 0 because Route 1 hits the deep flood zone!");
        assertEquals(1, resultWithFlood.relevantHazards().size(),
                "Corridor should include the flood hazard");

        // Test without hazards: Route 1 (Direct) should be index 0 because it's faster
        RouteSafetyEngine.SafetyEvaluationResult resultNoHazard = engine.evaluateAndSelectSafestRoute(
                List.of(route1, route2),
                List.of()
        );
        assertEquals("Direct Path", resultNoHazard.routes().get(0).summary(),
                "Route 1 (Direct) should be at index 0 when no hazards exist");
    }

    // Helper to generate a simple 2-point encoded polyline
    private String createStraightPolyline(double lat1, double lng1, double lat2, double lng2) {
        StringBuilder sb = new StringBuilder();
        encodePoint(lat1, lng1, 0, 0, sb);
        encodePoint(lat2, lng2, lat1, lng1, sb);
        return sb.toString();
    }

    private String createDetourPolyline() {
        StringBuilder sb = new StringBuilder();
        encodePoint(10.7600, 106.6600, 0, 0, sb);
        encodePoint(10.7630, 106.6650, 10.7600, 106.6600, sb);
        encodePoint(10.7600, 106.6700, 10.7630, 106.6650, sb);
        return sb.toString();
    }

    private void encodePoint(double lat, double lng, double prevLat, double prevLng, StringBuilder sb) {
        int dLat = (int) Math.round((lat - prevLat) * 1e5);
        int dLng = (int) Math.round((lng - prevLng) * 1e5);
        encodeSignedNumber(dLat, sb);
        encodeSignedNumber(dLng, sb);
    }

    private void encodeSignedNumber(int num, StringBuilder sb) {
        int sgn_num = num < 0 ? ~(num << 1) : (num << 1);
        while (sgn_num >= 0x20) {
            int nextValue = (0x20 | (sgn_num & 0x1f)) + 63;
            sb.append((char) nextValue);
            sgn_num >>= 5;
        }
        num = sgn_num + 63;
        sb.append((char) num);
    }

    private RouteResponse.RouteDto createRouteDto(String polylinePoints, long durationSeconds, long distanceMeters, String summary) {
        return RouteResponse.RouteDto.builder()
                .summary(summary)
                .overview_polyline(RouteResponse.OverviewPolyline.builder().points(polylinePoints).build())
                .legs(List.of(
                        RouteResponse.LegDto.builder()
                                .duration(RouteResponse.ValueText.builder().value((double) durationSeconds).text(durationSeconds + "s").build())
                                .distance(RouteResponse.ValueText.builder().value((double) distanceMeters).text(distanceMeters + "m").build())
                                .build()
                ))
                .build();
    }

    private HazardSpatialProjection createHazardMock(UUID id, HazardType type, double lat, double lng) {
        return new HazardSpatialProjection() {
            @Override
            public UUID getId() {
                return id;
            }

            @Override
            public HazardType getHazardType() {
                return type;
            }

            @Override
            public Double getLatitude() {
                return lat;
            }

            @Override
            public Double getLongitude() {
                return lng;
            }
        };
    }
}
