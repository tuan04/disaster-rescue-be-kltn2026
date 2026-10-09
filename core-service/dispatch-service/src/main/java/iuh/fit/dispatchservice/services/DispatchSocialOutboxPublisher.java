package iuh.fit.dispatchservice.services;


import iuh.fit.dispatchservice.entity.DispatchSocialOutbox;
import iuh.fit.dispatchservice.kafka.producer.SocialCreatedEventProducer;
import iuh.fit.dispatchservice.repositories.DispatchSocialOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchSocialOutboxPublisher {
    private static final int BATCH_SIZE = 20;

    // Lỗi gửi sẽ được thử lại sớm nhất sau 10 giây.
    private static final long RETRY_DELAY_SECONDS = 10;

    // Đọc và cập nhật PostgreSQL.
    private final DispatchSocialOutboxRepository outboxRepository;

    // Gửi dữ liệu sang Kafka.
    private final SocialCreatedEventProducer producer;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int publishPendingEvents() {

        // Chốt thời điểm lấy các sự kiện đã đến hạn.
        Instant now = Instant.now();

        // Đọc tối đa 20 bản ghi PENDING.
        // Thứ tự đã được khai báo trong JPQL của findDue().
        List<DispatchSocialOutbox> events = outboxRepository.findDue(
                now,
                PageRequest.of(0, BATCH_SIZE)
        );

        // Đếm số bản ghi lượt này thực sự chuyển sang SENT.
        int publishedCount = 0;

        // Xử lý lần lượt các sự kiện đã đọc.
        for (DispatchSocialOutbox event : events) {

            // Nếu ứng dụng đang yêu cầu dừng thread thì không gửi thêm.
            if (Thread.currentThread().isInterrupted()) {
                return publishedCount;
            }

            try {
                // Chỉ gửi payload đã lưu và commit từ bước tạo yêu cầu.
                // Không tạo lại eventId hoặc dựng lại JSON.
                producer.sendAndWait(
                        event.getTopic(),
                        event.getMessageKey(),
                        event.getPayload()
                );

            } catch (InterruptedException exception) {

                // get() ném InterruptedException nên cần khôi phục cờ interrupt.
                Thread.currentThread().interrupt();

                // Chưa có xác nhận rõ ràng nên giữ nguyên PENDING.
                log.info(
                        "Dung gui dispatch outbox, eventId={}",
                        event.getEventId()
                );

                // Kết thúc lượt gửi.
                return publishedCount;

            } catch (
                    ExecutionException |
                    TimeoutException |
                    RuntimeException exception
            ) {

                // Lấy lỗi bên trong ExecutionException nếu có.
                Throwable cause =
                        exception instanceof ExecutionException
                                && exception.getCause() != null
                                ? exception.getCause()
                                : exception;

                // Lưu tên loại lỗi, không lưu toàn bộ payload vào thông tin lỗi.
                String errorCode = cause.getClass().getSimpleName();

                // Giữ PENDING và hẹn lần thử tiếp theo.
                // Nếu bản ghi đã SENT hoặc lịch đã đổi thì update trả về 0.
                outboxRepository.deferPublish(
                        event.getEventId(),
                        event.getPublishNextAt(),
                        Instant.now().plusSeconds(RETRY_DELAY_SECONDS),
                        errorCode
                );

                // Ghi nhận sự kiện chưa gửi được.
                log.warn(
                        "Gui dispatch outbox chua thanh cong, eventId={}, error={}",
                        event.getEventId(),
                        errorCode
                );

                // Không chạy nhánh đánh dấu SENT cho sự kiện này.
                continue;
            }

            // Chỉ chạy đến đây nếu Kafka đã xác nhận thành công.
            // Đặt ngoài try/catch gửi Kafka để không nhầm lỗi database
            // thành lỗi Kafka.
            int updated = outboxRepository.markPublished(
                    event.getEventId(),
                    Instant.now()
            );

            // 1: chính lượt này chuyển được PENDING sang SENT.
            // 0: có thể worker khác đã đánh dấu SENT trước.
            if (updated == 1) {
                publishedCount++;
            }
        }

        // Trả số cập nhật thành công của lượt này.
        return publishedCount;
    }

}
