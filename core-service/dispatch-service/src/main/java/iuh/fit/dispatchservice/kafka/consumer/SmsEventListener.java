package iuh.fit.dispatchservice.kafka.consumer;

import iuh.fit.common.kafka.dto.SOSRequest;
import iuh.fit.dispatchservice.services.RescueService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmsEventListener {
    private final RescueService rescueService;

    @KafkaListener(topics = "sms-sos-topic")
    public void handleSmsEvent(SOSRequest event) {
        rescueService.createSOSRequest(event, null);
    }
}
