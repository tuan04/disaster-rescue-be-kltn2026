package iuh.fit.integration.dtos.response;


import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class WeatherContextResponse {

    // 1. Trạng thái thời tiết bằng chữ (VD: "Moderate or heavy rain shower")
    private String condition;

    // 2. Nhiệt độ thực tế (°C) - Đánh giá nguy cơ sốc nhiệt/hạ thân nhiệt
    private Double temperature;

    // 3. Tốc độ gió giật (km/h) - Quan trọng nhất để phát hiện bão, nguy cơ sập nhà
    private Double gustKph;

    // 4. Lượng mưa hiện tại (mm/giờ) - Đánh giá tốc độ ngập lụt tại thời điểm gọi SOS
    private Double precipMm;

    // 5. Tầm nhìn xa (km) - Đánh giá độ khó khi đội cứu hộ di chuyển
    private Double visibilityKm;

    // --- CÁC THÔNG SỐ CHUYÊN SÂU (Cho bão/lụt) ---

    // 6. Ngày hay Đêm (1 = Ngày, 0 = Đêm) - Cứu hộ ban đêm rủi ro cao hơn
    private Integer isDay;

    // 7. Áp suất khí quyển (mb) - Dưới 1000 mb là dấu hiệu của tâm bão
    private Double pressureMb;

    // 8. Tổng lượng mưa tích lũy trong ngày (mm) - Đánh giá nguy cơ no nước, sạt lở đất
    private Double totalPrecipMm;
}
