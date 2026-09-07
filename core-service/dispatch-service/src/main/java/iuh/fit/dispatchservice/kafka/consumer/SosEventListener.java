package iuh.fit.dispatchservice.kafka.consumer;


import iuh.fit.common.kafka.dto.AIEvaluationNotificationResponse;
import iuh.fit.dispatchservice.services.SOSService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SosEventListener {
    private final SOSService sosService;

    @KafkaListener(topics = "sos-evaluated-topic")
    public void handleSosEvaluated(AIEvaluationNotificationResponse event){
        System.out.println("Received AI evaluation event: " + event);
        sosService.updateSOSByAI(event);
    }
}
