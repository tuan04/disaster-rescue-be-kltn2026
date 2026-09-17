package iuh.fit.dispatchservice.dtos.response;

import lombok.Builder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;

@Builder
public record GeoJsonPolygonDto(
        String type,
        List<List<List<Double>>> coordinates
) {
    public static GeoJsonPolygonDto fromPolygon(Polygon polygon) {
        if (polygon == null || polygon.isEmpty()) {
            return null;
        }

        List<List<List<Double>>> coordinates = new ArrayList<>();

        LineString exteriorRing = polygon.getExteriorRing();
        if (exteriorRing != null && !exteriorRing.isEmpty()) {
            coordinates.add(extractRing(exteriorRing));
        }

        int numInteriorRings = polygon.getNumInteriorRing();
        for (int i = 0; i < numInteriorRings; i++) {
            LineString interiorRing = polygon.getInteriorRingN(i);
            if (interiorRing != null && !interiorRing.isEmpty()) {
                coordinates.add(extractRing(interiorRing));
            }
        }

        return new GeoJsonPolygonDto("Polygon", coordinates);
    }

    private static List<List<Double>> extractRing(LineString ring) {
        List<List<Double>> ringCoords = new ArrayList<>();
        for (Coordinate coord : ring.getCoordinates()) {
            ringCoords.add(List.of(coord.getX(), coord.getY()));
        }
        return ringCoords;
    }
}
