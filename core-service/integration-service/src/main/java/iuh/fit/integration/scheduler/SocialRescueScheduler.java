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

    private final SocialOutboxPublisher outboxPublisher;

    @Scheduled(
            scheduler = "socialTaskScheduler",
            fixedDelayString = "${social.jobs.approval-delay-ms:5000}",
            initialDelayString = "${social.jobs.initial-delay-ms:10000}"
    )
    public void approvePendingFeeds() {

        try {
            int approvedCount = approvalFlow.approvePendingFeeds();

            if (approvedCount > 0) {
                log.info(
                        "Tu dong phe duyet thanh cong {} feed",
                        approvedCount
                );
            }

        } catch (RuntimeException exception) {
            log.error(
                    "Luot tu dong phe duyet feed gap loi",
                    exception
            );
        }
    }


    @Scheduled(
            scheduler = "socialTaskScheduler",
            fixedDelayString = "${social.jobs.publish-delay-ms:2000}",
            initialDelayString = "${social.jobs.initial-delay-ms:10000}"
    )
    public void publishPendingEvents() {
        try {
            int publishedCount = outboxPublisher.publishPendingEvents();

            // Chỉ ghi log thông thường khi có cập nhật thành công.
            if (publishedCount > 0) {
                log.info(
                        "Da danh dau SENT cho {} su kien outbox",
                        publishedCount
                );
            }

        } catch (RuntimeException exception) {
            log.error(
                    "Luot gui social outbox gap loi",
                    exception
            );
        }
    }
}
