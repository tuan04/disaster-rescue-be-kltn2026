package iuh.fit.integration.kafka.producer;

import iuh.fit.common.kafka.dto.AIEvaluationNotificationResponse;
import iuh.fit.common.kafka.dto.SOSResponse;
import iuh.fit.common.response.ApiResponse;
import iuh.fit.integration.dtos.response.AiEvaluationResponse;
import iuh.fit.integration.dtos.response.WeatherContextResponse;
import iuh.fit.integration.services.AiService;
import iuh.fit.integration.services.ChatService;
import iuh.fit.integration.services.GeocodingService;
import iuh.fit.integration.services.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RescueEvaluateAIProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final AiService aiService;
    private final GeocodingService geocodingService;
    private final WeatherService weatherService;

    public void publishAIEvaluationNotification(SOSResponse sosResponse) {
        String address = geocodingService.getAddress(sosResponse.latitude(), sosResponse.longitude());

        WeatherContextResponse weatherContext = weatherService.getWeather(sosResponse.latitude(), sosResponse.longitude());
        AiEvaluationResponse aiEvaluation = aiService.evaluateEmergency(sosResponse.content(), weatherContext);

        AIEvaluationNotificationResponse event  =   new AIEvaluationNotificationResponse(
                sosResponse.id(),
                aiEvaluation.emergencyLevel(),
                aiEvaluation.evaluate(),
                address
        );

        kafkaTemplate.send("sos-evaluated-topic", event);
    }

}
