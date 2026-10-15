package iuh.fit.dispatchservice.repositories.specifications;

import iuh.fit.dispatchservice.dtos.request.RescueRequestFilterRequest;
import iuh.fit.dispatchservice.entity.MapPoint;
import iuh.fit.dispatchservice.entity.RescueRequest;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class RescueRequestSearchSpecification {

    private RescueRequestSearchSpecification() {}

    public static Specification<RescueRequest> filter(
            RescueRequestFilterRequest request
    ) {
        // Tái sử dụng bộ lọc ba trường đã có trong dự án.
        Specification<RescueRequest> base =
                RescueRequestSpecification.filter(
                        request.getStatus(),
                        request.getEmergencyLevel(),
                        request.getSource()
                );

        // Bổ sung điều kiện khu vực và tìm kiếm.
        return base.and((root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Bỏ khoảng trắng ở đầu/cuối từ khóa.
            String keyword = request.getKeyword() == null
                    ? ""
                    : request.getKeyword().strip();

            // Chỉ nối MapPoint khi cần lọc khu vực hoặc tìm địa chỉ.
            if (request.getLocationId() != null || !keyword.isEmpty()) {

                // LEFT JOIN giữ lại yêu cầu để vẫn có thể tìm theo phone,
                // ngay cả khi không có dữ liệu phía MapPoint.
                Join<RescueRequest, MapPoint> mapPoint =
                        root.join("mapPoint", JoinType.LEFT);

                // locationId thuộc MapPoint, không thuộc RescueRequest.
                if (request.getLocationId() != null) {
                    predicates.add(
                            cb.equal(
                                    mapPoint.get("locationId"),
                                    request.getLocationId()
                            )
                    );
                }

                if (!keyword.isEmpty()) {

                    // Chuyển chữ thường để tìm địa chỉ không phân biệt hoa/thường.
                    // Escape các ký tự đặc biệt của LIKE.
                    String pattern = "%"
                            + escapeLike(keyword.toLowerCase(Locale.ROOT))
                            + "%";

                    // Địa chỉ chứa từ khóa.
                    Predicate addressMatches = cb.like(
                            cb.lower(mapPoint.<String>get("address")),
                            pattern,
                            '!'
                    );

                    // Số điện thoại chứa từ khóa.
                    Predicate phoneMatches = cb.like(
                            root.<String>get("reporterPhone"),
                            pattern,
                            '!'
                    );

                    predicates.add(
                            cb.or(addressMatches, phoneMatches)
                    );
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }

    private static String escapeLike(String value) {
        // Dùng ! làm ký tự escape:
        // % và _ trong từ khóa được tìm như ký tự bình thường,
        // không biến thành wildcard SQL.
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
