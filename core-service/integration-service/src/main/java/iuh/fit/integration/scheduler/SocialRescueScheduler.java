package iuh.fit.integration.scheduler;


import iuh.fit.integration.services.SocialApprovalFlow;
import iuh.fit.integration.services.SocialOutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SocialRescueScheduler {
    private final SocialApprovalFlow approvalFlow;

    // Phụ trách gửi những sự kiện đã có trong outbox.
    private final SocialOutboxPublisher outboxPublisher;

    // Khai báo lịch cho tác vụ xét duyệt.
    @Scheduled(
            // Chạy trên scheduler có hai thread đã tạo ở bước trên.
            scheduler = "socialTaskScheduler",

            // Đợi sau khi lượt trước kết thúc rồi mới chạy lượt tiếp theo.
            // Nếu chưa có cấu hình YAML thì dùng mặc định 5000 ms.
            fixedDelayString = "${social.jobs.approval-delay-ms:5000}",

            // Khoảng chờ trước lần chạy đầu tiên khi lịch được đăng ký.
            // Đây không phải thao tác kiểm tra database/Kafka đã sẵn sàng.
            initialDelayString = "${social.jobs.initial-delay-ms:10000}"
    )
    public void approvePendingFeeds() {

        try {
            // Service tự đọc policy và chỉ duyệt khi enabled = true.
            // Kết quả là số feed thực sự chuyển sang APPROVED trong lượt này.
            int approvedCount = approvalFlow.approvePendingFeeds();

            // Chỉ ghi log thông thường khi có feed vừa được duyệt.
            // Tránh ghi một dòng "0 feed" sau mỗi vài giây.
            if (approvedCount > 0) {
                log.info(
                        "Tu dong phe duyet thanh cong {} feed",
                        approvedCount
                );
            }

        } catch (RuntimeException exception) {

            // Ghi cả stack trace để tìm nguyên nhân như lỗi MongoDB.
            // Không tự đổi trạng thái những feed đang xử lý dở.
            log.error(
                    "Luot tu dong phe duyet feed gap loi",
                    exception
            );

            // Kết thúc lượt hiện tại.
            // Lượt theo lịch tiếp theo sẽ đọc lại dữ liệu trong database.
        }
    }

    // Khai báo lịch riêng cho tác vụ gửi outbox.
    @Scheduled(
            // Dùng cùng nhóm thread, nhưng đây là một tác vụ độc lập.
            scheduler = "socialTaskScheduler",

            // Nghỉ 2000 ms sau khi gửi xong một lượt nếu YAML không ghi đè.
            fixedDelayString = "${social.jobs.publish-delay-ms:2000}",

            // Khoảng chờ trước lần chạy đầu tiên.
            initialDelayString = "${social.jobs.initial-delay-ms:10000}"
    )
    public void publishPendingEvents() {
        try {
            // Gửi những outbox đã đến hạn và chờ Kafka xác nhận.
            // Kết quả là số outbox lượt này đánh dấu được thành SENT.
            int publishedCount = outboxPublisher.publishPendingEvents();

            // Chỉ ghi log thông thường khi có cập nhật thành công.
            if (publishedCount > 0) {
                log.info(
                        "Da danh dau SENT cho {} su kien outbox",
                        publishedCount
                );
            }

        } catch (RuntimeException exception) {

            // Ví dụ: Kafka đã nhận nhưng cập nhật MongoDB bị lỗi.
            // Không đánh dấu SENT tại scheduler để che đi lỗi đó.
            log.error(
                    "Luot gui social outbox gap loi",
                    exception
            );

            // Lượt sau truy vấn lại MongoDB để biết sự kiện nào còn PENDING.
        }
    }
}
