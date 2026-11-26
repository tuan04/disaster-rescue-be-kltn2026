package iuh.fit.dispatchservice.kafka.producer;

import iuh.fit.common.kafka.dto.RescueAcceptedEvent;
import iuh.fit.common.kafka.dto.RescueAssignedEvent;
import iuh.fit.common.kafka.dto.RescueRejectedEvent;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import iuh.fit.common.kafka.dto.RescueCanceledEvent;
import iuh.fit.common.kafka.dto.RescueCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RescueEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishRescueRejectedEvent(RescueRejectedEvent event) {
        try {
            kafkaTemplate.send(RescueRejectedEvent.TOPIC_NAME, event.teamId().toString(), event)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.error("Failed to publish rescue rejection {}", event.assignmentId(), exception);
                        }
                    });
        } catch (RuntimeException exception) {
            log.error("Failed to publish rescue rejection {}", event.assignmentId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishRescueAssignedEvent(RescueAssignedEvent event) {
        try {
            kafkaTemplate.send(RescueAssignedEvent.TOPIC_NAME, event.teamId().toString(), event)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.error("Failed to publish rescue assignment {}", event.assignmentId(), exception);
                        }
                    });
        } catch (RuntimeException exception) {
            log.error("Failed to publish rescue assignment {}", event.assignmentId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishRescueAcceptedEvent(RescueAcceptedEvent event) {
        log.info("Publishing RescueAcceptedEvent to topic {}: {}", RescueAcceptedEvent.TOPIC_NAME, event);
        try {
            kafkaTemplate.send(RescueAcceptedEvent.TOPIC_NAME, event.teamId().toString(), event)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.error("Failed to publish rescue acceptance {}", event.assignmentId(), exception);
                        }
                    });
        } catch (RuntimeException exception) {
            log.error("Failed to publish rescue acceptance {}", event.assignmentId(), exception);
        }
    }

    public void publishRescueCompletedEvent(RescueCompletedEvent event) {
        log.info("Publishing RescueCompletedEvent to topic {}: {}", RescueCompletedEvent.TOPIC_NAME, event);
        kafkaTemplate.send(
                RescueCompletedEvent.TOPIC_NAME,
                event.campaignTeamId().toString(),
                event);
    }

    public void publishRescueCanceledEvent(RescueCanceledEvent event) {
        log.info("Publishing RescueCanceledEvent to topic {}: {}", RescueCanceledEvent.TOPIC_NAME, event);
        kafkaTemplate.send(
                RescueCanceledEvent.TOPIC_NAME,
                event.campaignTeamId().toString(),
                event);
    }
}
