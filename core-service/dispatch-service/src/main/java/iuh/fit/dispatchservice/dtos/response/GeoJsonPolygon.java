package iuh.fit.dispatchservice.dtos.response;

import lombok.Builder;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.List;


@Builder
public record GeoJsonPolygon(
        String type,
        List<List<List<Double>>> coordinates
) {

    /**
     * Chuyển đổi từ đối tượng không gian JTS {@link Polygon} (lưu trong Database PostGIS)
     * sang đối tượng {@link GeoJsonPolygon} để tuần tự hóa (serialize) thành JSON gửi về Client.
     *
     * @param polygon Đối tượng JTS Polygon cần chuyển đổi (có thể null hoặc rỗng)
     * @return Đối tượng {@link GeoJsonPolygon} chuẩn GeoJSON, hoặc {@code null} nếu polygon đầu vào rỗng/null
     */
    public static GeoJsonPolygon fromPolygon(Polygon polygon) {
        // Kiểm tra đa giác rỗng hoặc null
        if (polygon == null || polygon.isEmpty()) {
            return null;
        }

        List<List<List<Double>>> coordinates = new ArrayList<>();

        // 1. Trích xuất vòng ranh giới bao ngoài (Exterior Ring / Shell)
        LineString exteriorRing = polygon.getExteriorRing();
        if (exteriorRing != null && !exteriorRing.isEmpty()) {
            coordinates.add(extractRing(exteriorRing));
        }

        // 2. Trích xuất tất cả các vòng lỗ rỗng bên trong (Interior Rings / Holes) nếu có
        int numInteriorRings = polygon.getNumInteriorRing();
        for (int i = 0; i < numInteriorRings; i++) {
            LineString interiorRing = polygon.getInteriorRingN(i);
            if (interiorRing != null && !interiorRing.isEmpty()) {
                coordinates.add(extractRing(interiorRing));
            }
        }

        // Đóng gói thành GeoJSON Polygon hợp lệ với type = "Polygon"
        return new GeoJsonPolygon("Polygon", coordinates);
    }

    /**
     * Chuyển đổi từ cấu trúc dữ liệu GeoJSON nhận từ Client (Request Body)
     * thành đối tượng JTS {@link Polygon} để lưu trữ vào database PostGIS với hệ tọa độ WGS 84 (SRID 4326).
     *
     * @param geometryFactory Factory dùng để khởi tạo đối tượng hình học JTS (GeometryFactory với SRID 4326)
     * @return Đối tượng JTS {@link Polygon} hoàn chỉnh với SRID 4326, hoặc {@code null} nếu tọa độ không hợp lệ
     */
    public Polygon toPolygon(GeometryFactory geometryFactory) {
        // Kiểm tra tính hợp lệ ban đầu của mảng tọa độ
        if (coordinates == null || coordinates.isEmpty()) {
            return null;
        }

        // Vòng đầu tiên (index = 0) bắt buộc phải là đường ranh giới bao ngoài (Exterior Ring)
        List<List<Double>> exteriorCoords = coordinates.get(0);
        if (exteriorCoords == null || exteriorCoords.size() < 3) {
            return null;
        }

        // Trích xuất danh sách điểm Coordinate[] cho vòng ngoài
        Coordinate[] shellCoords = extractCoordinates(exteriorCoords);
        // Một LinearRing hợp lệ theo quy tắc topo phải có tối thiểu 4 điểm (3 đỉnh tam giác + 1 điểm đóng vòng lặp)
        if (shellCoords.length < 4) {
            return null;
        }

        // Khởi tạo vòng bao ngoài khép kín (LinearRing shell)
        LinearRing shell = geometryFactory.createLinearRing(shellCoords);

        // Xử lý các vòng ranh giới lỗ thủng rỗng bên trong (Holes) nếu Client có gửi kèm
        LinearRing[] holes = null;
        if (coordinates.size() > 1) {
            List<LinearRing> holeList = new ArrayList<>();
            for (int i = 1; i < coordinates.size(); i++) {
                List<List<Double>> interiorCoords = coordinates.get(i);
                Coordinate[] holeCoords = extractCoordinates(interiorCoords);
                if (holeCoords.length >= 4) {
                    holeList.add(geometryFactory.createLinearRing(holeCoords));
                }
            }
            if (!holeList.isEmpty()) {
                holes = holeList.toArray(new LinearRing[0]);
            }
        }

        // Tạo đối tượng Polygon từ shell và danh sách holes
        Polygon polygon = geometryFactory.createPolygon(shell, holes);

        // Gán mã hệ tọa độ quy chiếu SRID = 4326 (WGS 84 - GPS chuẩn toàn cầu phù hợp PostGIS)
        polygon.setSRID(4326);
        return polygon;
    }

    /**
     * Hàm trợ năng: Trích xuất và chuẩn hóa danh sách các điểm tọa độ từ định dạng mảng số thực
     * sang mảng đối tượng {@link Coordinate} của JTS.
     * <p>
     * <b>Cơ chế tự động khép góc (Auto-close Ring):</b>
     * Thư viện JTS yêu cầu một {@link LinearRing} bắt buộc điểm đầu và điểm cuối phải có cùng tọa độ (khép kín).
     * Nếu dữ liệu gửi lên từ Client thiếu điểm đóng (điểm cuối khác điểm đầu), hàm sẽ tự động lặp lại điểm đầu
     * vào vị trí cuối mảng để tránh phát sinh ngoại lệ {@code IllegalArgumentException: Points of LinearRing do not form a closed linestring}.
     *
     * @param ringList Danh sách các điểm tọa độ dạng [[lng1, lat1], [lng2, lat2], ...]
     * @return Mảng {@link Coordinate} đã được chuẩn hóa và đảm bảo khép kín
     */
    private static Coordinate[] extractCoordinates(List<List<Double>> ringList) {
        if (ringList == null || ringList.isEmpty()) {
            return new Coordinate[0];
        }

        List<Coordinate> coords = new ArrayList<>();
        for (List<Double> pt : ringList) {
            if (pt != null && pt.size() >= 2) {
                // GeoJSON format: index 0 là Longitude (kinh độ X), index 1 là Latitude (vĩ độ Y)
                coords.add(new Coordinate(pt.get(0), pt.get(1)));
            }
        }

        // Đảm bảo tính khép kín của vòng: Điểm đầu tiên và điểm cuối cùng phải trùng tọa độ 2D
        if (!coords.isEmpty()) {
            Coordinate first = coords.get(0);
            Coordinate last = coords.get(coords.size() - 1);
            if (!first.equals2D(last)) {
                coords.add(new Coordinate(first.x, first.y));
            }
        }

        return coords.toArray(new Coordinate[0]);
    }

    /**
     * Hàm trợ năng: Trích xuất các tọa độ từ một đường gấp khúc {@link LineString} của JTS
     * và định dạng thành danh sách các cặp số [X, Y] chuẩn format GeoJSON.
     *
     * @param ring Đối tượng LineString (thường là exteriorRing hoặc interiorRing của Polygon)
     * @return Danh sách các điểm [[X1, Y1], [X2, Y2], ...] biểu diễn cho một vòng khép kín
     */
    private static List<List<Double>> extractRing(LineString ring) {
        List<List<Double>> ringCoords = new ArrayList<>();
        for (Coordinate coord : ring.getCoordinates()) {
            // coord.getX() là Kinh độ (Longitude), coord.getY() là Vĩ độ (Latitude)
            ringCoords.add(List.of(coord.getX(), coord.getY()));
        }
        return ringCoords;
    }
}
