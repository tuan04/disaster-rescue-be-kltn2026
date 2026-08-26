package iuh.fit.integration.kafka.consumer;

import iuh.fit.integration.dtos.SOSResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EvaluateSituation {

    @KafkaListener(topics = "sos-event", groupId = "ai-evaluation-group")
    public void evaluateSituation(SOSResponse event) {
        log.info("Received SOS event in integration-service for AI evaluation: {}", event);
        // AI nhận dữ liệu và bắt đầu phân tích tình huống
        log.info("AI evaluation triggered for SOS request ID: {}", event.getId());
    }
}
