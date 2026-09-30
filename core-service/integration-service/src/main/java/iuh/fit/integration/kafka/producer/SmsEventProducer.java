package iuh.fit.integration.kafka.producer;

import iuh.fit.common.kafka.dto.SOSRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmsEventProducer {
    private static final String TOPIC_NAME = "sms-sos-topic";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSmsEvent(SOSRequest smsEvent) {
        kafkaTemplate.send(TOPIC_NAME, smsEvent);
    }
}
