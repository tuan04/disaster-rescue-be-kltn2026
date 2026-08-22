package iuh.fit.dispatchservice.kafka.producer;

import iuh.fit.dispatchservice.dtos.response.SOSResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SosEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSOSEvent(SOSResponse sosResponse) {
        kafkaTemplate.send(
                "sos-event",
                sosResponse.id().toString(),
                sosResponse
        );
    }
}
