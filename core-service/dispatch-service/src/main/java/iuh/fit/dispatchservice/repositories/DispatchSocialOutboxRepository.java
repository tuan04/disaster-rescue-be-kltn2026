package iuh.fit.dispatchservice.repositories;

import iuh.fit.dispatchservice.entity.DispatchSocialOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DispatchSocialOutboxRepository extends JpaRepository<DispatchSocialOutbox, UUID> {

    @Transactional(readOnly = true)
    @Query("""
            SELECT o
            FROM DispatchSocialOutbox o
            WHERE o.publishState = 'PENDING'
              AND o.publishNextAt <= :now
            ORDER BY o.publishNextAt ASC, o.eventId ASC
            """)
    List<DispatchSocialOutbox> findDue(
            // Chỉ lấy những sự kiện đã đến hạn ở thời điểm này.
            @Param("now") Instant now,
            // Pageable giới hạn số bản ghi, không phải tham số trong JPQL.
            Pageable pageable
    );


    @Modifying
    @Transactional
    @Query("""
            UPDATE DispatchSocialOutbox o
            SET o.publishState = 'SENT',
                o.publishedAt = :publishedAt,
                o.publishNextAt = NULL,
                o.lastPublishError = NULL
            WHERE o.eventId = :eventId
              AND o.publishState = 'PENDING'
            """)
    int markPublished(
            // ID của đúng sự kiện vừa được Kafka xác nhận.
            @Param("eventId") UUID eventId,

            // Thời điểm ứng dụng ghi nhận xác nhận thành công.
            @Param("publishedAt") Instant publishedAt
    );


    @Modifying
    @Transactional
    @Query("""
            UPDATE DispatchSocialOutbox o
            SET o.publishNextAt = :nextAttemptAt,
                o.lastPublishError = :errorCode
            WHERE o.eventId = :eventId
              AND o.publishState = 'PENDING'
              AND o.publishNextAt = :observedNextAt
            """)
    int deferPublish(
            // Xác định sự kiện vừa gửi lỗi.
            @Param("eventId") UUID eventId,

            // Giá trị publishNextAt tại thời điểm worker đọc bản ghi.
            @Param("observedNextAt") Instant observedNextAt,

            // Thời điểm sớm nhất được phép gửi lại.
            @Param("nextAttemptAt") Instant nextAttemptAt,

            // Lưu loại lỗi của lần gửi.
            @Param("errorCode") String errorCode
    );



}
