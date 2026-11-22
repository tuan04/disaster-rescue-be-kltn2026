package iuh.fit.dispatchservice.kafka.producer;

import iuh.fit.common.kafka.dto.MapPointCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MapPointEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(MapPointCreatedEvent event) {
        try {
            kafkaTemplate.send(
                    MapPointCreatedEvent.TOPIC,
                    event.mapPointId().toString(),
                    event
            ).whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error(
                            "Gửi Kafka thất bại: topic={}, mapPointId={}",
                            MapPointCreatedEvent.TOPIC,
                            event.mapPointId(),
                            exception
                    );
                    return;
                }

                var metadata = result.getRecordMetadata();

                log.info(
                        "Gửi Kafka thành công: topic={}, mapPointId={}, partition={}, offset={}",
                        metadata.topic(),
                        event.mapPointId(),
                        metadata.partition(),
                        metadata.offset()
                );
            });

        } catch (RuntimeException exception) {
            log.error(
                    "Lỗi khi gọi gửi Kafka: mapPointId={}",
                    event.mapPointId(),
                    exception
            );
        }
    }
}