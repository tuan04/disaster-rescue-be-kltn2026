package iuh.fit.dispatchservice.services;


import iuh.fit.dispatchservice.entity.DispatchSocialOutbox;
import iuh.fit.dispatchservice.kafka.producer.SocialCreatedEventProducer;
import iuh.fit.dispatchservice.repositories.DispatchSocialOutboxRepository;
import lombok.RequiredArgsConstructor;
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
public class DispatchSocialOutboxPublisher {
    private static final int BATCH_SIZE = 20;

    private static final long RETRY_DELAY_SECONDS = 10;

    private final DispatchSocialOutboxRepository outboxRepository;

    private final SocialCreatedEventProducer producer;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int publishPendingEvents() {

        Instant now = Instant.now();

        List<DispatchSocialOutbox> events = outboxRepository.findDue(
                now,
                PageRequest.of(0, BATCH_SIZE)
        );

        int publishedCount = 0;

        for (DispatchSocialOutbox event : events) {

            if (Thread.currentThread().isInterrupted()) {
                return publishedCount;
            }

            try {
                producer.sendAndWait(
                        event.getTopic(),
                        event.getMessageKey(),
                        event.getPayload()
                );

            } catch (InterruptedException exception) {

                // get() ném InterruptedException nên cần khôi phục cờ interrupt.
                Thread.currentThread().interrupt();

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

                String errorCode = cause.getClass().getSimpleName();

                outboxRepository.deferPublish(
                        event.getEventId(),
                        event.getPublishNextAt(),
                        Instant.now().plusSeconds(RETRY_DELAY_SECONDS),
                        errorCode
                );

                continue;
            }


            int updated = outboxRepository.markPublished(
                    event.getEventId(),
                    Instant.now()
            );

            if (updated == 1) {
                publishedCount++;
            }
        }

        return publishedCount;
    }

}
