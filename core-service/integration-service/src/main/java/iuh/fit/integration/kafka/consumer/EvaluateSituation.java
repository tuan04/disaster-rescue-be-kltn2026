package iuh.fit.integration.kafka.consumer;

import iuh.fit.common.kafka.dto.SOSResponse;
import iuh.fit.integration.kafka.producer.RescueEvaluateAIProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EvaluateSituation {

    private final RescueEvaluateAIProducer rescueEvaluateAIProducer;


    @KafkaListener(topics = "sos-event", groupId = "ai-evaluation-group")
    public void evaluateSituation(SOSResponse event) {
        log.info("Received SOS event: {}", event);
        rescueEvaluateAIProducer.publishAIEvaluationNotification(event);
    }
}
