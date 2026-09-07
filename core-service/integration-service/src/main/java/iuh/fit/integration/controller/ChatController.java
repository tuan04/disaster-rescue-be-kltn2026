package iuh.fit.integration.controller;

import iuh.fit.common.kafka.dto.AIEvaluationNotificationResponse;
import iuh.fit.common.kafka.dto.SOSResponse;
import iuh.fit.common.response.ApiResponse;
import iuh.fit.integration.dtos.response.AiEvaluationResponse;
import iuh.fit.integration.dtos.response.NominatimResponse;
import iuh.fit.integration.dtos.response.WeatherContextResponse;
import iuh.fit.integration.services.AiService;
import iuh.fit.integration.services.ChatService;
import iuh.fit.integration.services.GeocodingService;
import iuh.fit.integration.services.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final AiService aiService;
    private final GeocodingService geocodingService;
    private final WeatherService weatherService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> chat(@RequestParam String prompt) {
        String response = chatService.chat(prompt);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/evaluate-test")
    public ResponseEntity<ApiResponse<AIEvaluationNotificationResponse>> evaluateTest(@RequestBody SOSResponse sosRequest) {
        String address = geocodingService.getAddress(sosRequest.latitude(), sosRequest.longitude());
        System.out.println("Address: " + address);

        WeatherContextResponse weatherContext = weatherService.getWeather(sosRequest.latitude(), sosRequest.longitude());
        System.out.println("weatherContext: " + weatherContext);

        AiEvaluationResponse aiEvaluation = aiService.evaluateEmergency(sosRequest.content(), weatherContext);
        System.out.println("aiEvaluation: " + aiEvaluation);

        return ResponseEntity.ok(ApiResponse.success(new AIEvaluationNotificationResponse(
                sosRequest.id(),
                aiEvaluation.emergencyLevel(),
                aiEvaluation.evaluate(),
                address
        )));
    }
}
