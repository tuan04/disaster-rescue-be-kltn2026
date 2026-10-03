package iuh.fit.dispatchservice.kafka.consumer;

import iuh.fit.common.kafka.dto.SOSRequest;
import iuh.fit.dispatchservice.services.SOSService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmsEventListener {
    private final SOSService sosService;

    @KafkaListener(topics = "sms-sos-topic")
    public void handleSmsEvent(SOSRequest event) {
        sosService.createSOSRequest(event, null);
    }
}
