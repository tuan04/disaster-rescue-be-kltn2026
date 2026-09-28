package iuh.fit.dispatchservice.utils;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.operation.distance.DistanceOp;

import java.util.ArrayList;
import java.util.List;

public final class PolylineUtils {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private PolylineUtils() {
    }

    public static List<Coordinate> decode(String encodedPath) {
        List<Coordinate> coordinates = new ArrayList<>();
        if (encodedPath == null || encodedPath.isBlank()) {
            return coordinates;
        }

        int index = 0;
        int len = encodedPath.length();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = encodedPath.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encodedPath.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            double latitude = lat / 1E5;
            double longitude = lng / 1E5;
            // JTS Coordinate: x = longitude, y = latitude
            coordinates.add(new Coordinate(longitude, latitude));
        }

        return coordinates;
    }

    public static LineString decodeToLineString(String encodedPath) {
        List<Coordinate> coords = decode(encodedPath);
        if (coords.isEmpty()) {
            return null;
        }
        if (coords.size() == 1) {
            coords.add(new Coordinate(coords.get(0).x, coords.get(0).y));
        }
        return GEOMETRY_FACTORY.createLineString(coords.toArray(new Coordinate[0]));
    }

    public static double calculateDistanceInMeters(LineString lineString, double pointLng, double pointLat) {
        if (lineString == null || lineString.isEmpty()) {
            return Double.MAX_VALUE;
        }

        double radLat = Math.toRadians(pointLat);
        double metersPerLat = 111320.0;
        double metersPerLng = 111320.0 * Math.cos(radLat);

        Coordinate[] coords = lineString.getCoordinates();
        Coordinate[] metricCoords = new Coordinate[coords.length];
        for (int i = 0; i < coords.length; i++) {
            double dx = (coords[i].x - pointLng) * metersPerLng;
            double dy = (coords[i].y - pointLat) * metersPerLat;
            metricCoords[i] = new Coordinate(dx, dy);
        }

        LineString metricLine = GEOMETRY_FACTORY.createLineString(metricCoords);
        Point metricPoint = GEOMETRY_FACTORY.createPoint(new Coordinate(0.0, 0.0));

        return DistanceOp.distance(metricLine, metricPoint);
    }
}
