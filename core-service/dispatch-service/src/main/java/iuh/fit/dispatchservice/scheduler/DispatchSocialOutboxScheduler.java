package iuh.fit.dispatchservice.scheduler;


import iuh.fit.dispatchservice.services.DispatchSocialOutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Spring quản lý class để phát hiện các method @Scheduled.
@Component

// Spring inject publisher thông qua constructor do Lombok tạo.
@RequiredArgsConstructor

// Cho phép dùng log.info() và log.error().
@Slf4j
public class DispatchSocialOutboxScheduler {
    private final DispatchSocialOutboxPublisher publisher;
    // Đăng ký tác vụ gửi outbox theo lịch.
    @Scheduled(
            // Chọn scheduler đã tạo trong class cấu hình.
            scheduler = "dispatchSocialTaskScheduler",

            // Nghỉ sau khi lượt trước kết thúc.
            // Nếu YAML chưa cấu hình thì dùng mặc định 2000 ms.
            fixedDelayString =
                    "${social.outbox.publish-delay-ms:2000}",

            // Chờ trước lần chạy đầu tiên khi lịch được đăng ký.
            // Nếu YAML chưa cấu hình thì dùng mặc định 10000 ms.
            initialDelayString =
                    "${social.outbox.initial-delay-ms:10000}"
    )
    public void publishOutbox() {

        try {
            // Publisher tự đọc dữ liệu, gửi Kafka và cập nhật kết quả.
            // Giá trị trả về là số bản ghi lượt này chuyển được sang SENT.
            int publishedCount = publisher.publishPendingEvents();

            // Chỉ ghi log thông thường khi có cập nhật thành công.
            // Không ghi log liên tục cho những lượt không có dữ liệu.
            if (publishedCount > 0) {
                log.info(
                        "Dispatch da danh dau SENT cho {} su kien social",
                        publishedCount
                );
            }

        } catch (RuntimeException exception) {

            // Ví dụ: truy vấn PostgreSQL hoặc cập nhật SENT thất bại.
            // Ghi cả stack trace để tìm nguyên nhân.
            log.error(
                    "Luot gui social outbox cua dispatch gap loi",
                    exception
            );

            // Không tự đánh dấu SENT hoặc xóa outbox ở đây.
            // Kết thúc lượt; lượt sau sẽ đọc lại trạng thái trong database.
        }
    }
}
