package iuh.fit.dispatchservice.scheduler;


import iuh.fit.dispatchservice.services.DispatchSocialOutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DispatchSocialOutboxScheduler {
    private final DispatchSocialOutboxPublisher publisher;

    @Scheduled(

            scheduler = "dispatchSocialTaskScheduler",

            fixedDelayString =
                    "${social.outbox.publish-delay-ms:2000}",

            initialDelayString =
                    "${social.outbox.initial-delay-ms:10000}"
    )
    public void publishOutbox() {
        int publishedCount = publisher.publishPendingEvents();

        if (publishedCount > 0) {
            log.info(
                    "Dispatch da danh dau SENT cho {} su kien social",
                    publishedCount
            );
        }
    }
}
