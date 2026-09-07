package iuh.fit.integration.services;


import iuh.fit.integration.dtos.response.AiEvaluationResponse;
import iuh.fit.integration.dtos.response.WeatherContextResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiService {
    private final ChatClient chatClient;

    public AiEvaluationResponse evaluateEmergency(String userContent, WeatherContextResponse w){
        String promptText = String.format(
                "Bạn là chuyên gia điều phối cứu hộ. Hãy đánh giá ca cấp cứu này.\n" +
                        "- Lời kêu cứu: '%s'\n" +
                        "- Thời tiết: %s. Nhiệt độ: %s°C, Gió giật: %s km/h, Mưa: %s mm/h, Áp suất: %s mb.\n\n" +
                        "QUY ĐỊNH BẮT BUỘC:\n" +
                        "- Trường emergencyLevel CHỈ ĐƯỢC PHÉP nhận một trong đúng 3 giá trị: \"LOW\", \"MEDIUM\", hoặc \"HIGH\". Tuyệt đối không dùng giá trị khác (như CRITICAL).\n" +
                        "- Trường evaluate phải là lời giải thích ngắn gọn bằng tiếng Việt dưới 30 chữ.",
                userContent, w.getCondition(), w.getTemperature(),
                w.getGustKph(), w.getPrecipMm(), w.getPressureMb()
        );

        return chatClient.prompt()
                .user(promptText)
                .call()
                .entity(AiEvaluationResponse.class);
    }
}
