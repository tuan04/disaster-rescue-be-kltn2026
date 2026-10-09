package iuh.fit.integration.services;

import iuh.fit.integration.entity.SocialRawFeed;
import iuh.fit.integration.kafka.producer.SocialApprovedEventProducer;
import iuh.fit.integration.repository.SocialFeedStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;


@Service
@RequiredArgsConstructor
@Slf4j
public class SocialOutboxPublisher {
    // Mỗi lượt đọc tối đa 20 sự kiện để giới hạn lượng công việc.
    private static final int BATCH_SIZE = 20;

    // Lần gửi lỗi sẽ được thử lại sớm nhất sau 10 giây.
    private static final long RETRY_DELAY_SECONDS = 10;

    private final SocialFeedStateRepository stateRepository;
    private final SocialApprovedEventProducer producer;

    public int publishPendingEvents() {

        // Chốt thời điểm truy vấn của lượt hiện tại.
        Instant now = Instant.now();

        // Lấy những outbox đang PENDING và đã đến hạn gửi.
        List<SocialRawFeed> feeds = stateRepository.findDueOutbox(
                now,
                BATCH_SIZE
        );

        // Chưa có bản ghi nào được đánh dấu thành công.
        int publishedCount = 0;

        // Gửi lần lượt từng sự kiện trong nhóm đã đọc.
        for (SocialRawFeed feed : feeds) {

            // Khi ứng dụng đang dừng, không bắt đầu gửi sự kiện mới.
            if (Thread.currentThread().isInterrupted()) {
                return publishedCount;
            }

            try {
                // Chỉ gửi dữ liệu đã được lưu vào MongoDB.
                // Không lấy extractedData để dựng lại sự kiện.
                producer.sendAndWait(
                        feed.getId(),
                        feed.getApprovalPayload()
                );

            } catch (InterruptedException exception) {

                // Khôi phục cờ interrupt vì get() đã ném InterruptedException.
                Thread.currentThread().interrupt();

                // Không đánh dấu SENT vì chưa có kết quả xác nhận rõ ràng.
                // Outbox vẫn PENDING để xử lý ở lần chạy sau.
                log.info(
                        "Dung gui outbox do thread bi interrupt, feedId={}",
                        feed.getId()
                );

                // Kết thúc lượt gửi để ứng dụng có thể dừng.
                return publishedCount;

            } catch (
                    ExecutionException |
                    TimeoutException |
                    RuntimeException exception
            ) {

                // ExecutionException bọc lỗi thực tế bên trong cause.
                // Chọn lỗi bên trong nếu có để ghi mã lỗi hữu ích hơn.
                Throwable cause =
                        exception instanceof ExecutionException
                                && exception.getCause() != null
                                ? exception.getCause()
                                : exception;

                // Chỉ lưu tên loại lỗi; không đưa toàn bộ payload vào MongoDB/log.
                String errorCode = cause.getClass().getSimpleName();

                // Hẹn thử lại và giữ nguyên trạng thái PENDING.
                // Nếu MongoDB lỗi tại đây, exception sẽ thoát khỏi hàm.
                stateRepository.deferPublish(
                        feed,
                        errorCode,
                        Instant.now().plusSeconds(RETRY_DELAY_SECONDS)
                );

                // Ghi nhận lỗi để theo dõi.
                log.warn(
                        "Gui outbox chua thanh cong, feedId={}, eventId={}, error={}",
                        feed.getId(),
                        feed.getApprovalEventId(),
                        errorCode
                );

                // Bỏ qua nhánh đánh dấu SENT và xử lý feed tiếp theo.
                continue;
            }

            // Chỉ chạy đến đây nếu sendAndWait() hoàn thành thành công.
            // Đặt thao tác MongoDB ngoài khối catch lỗi gửi Kafka:
            // Kafka thành công nhưng MongoDB lỗi là hai tình huống khác nhau.
            boolean updated = stateRepository.markPublished(
                    feed,
                    Instant.now()
            );

            // Chỉ đếm khi chính lượt này đổi được PENDING thành SENT.
            // false có thể do worker khác đã cập nhật trước.
            if (updated) {
                publishedCount++;
            }
        }

        // Trả kết quả của lượt gửi hiện tại.
        return publishedCount;
    }
}
