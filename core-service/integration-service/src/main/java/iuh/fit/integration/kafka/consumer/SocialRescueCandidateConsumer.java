package iuh.fit.integration.kafka.consumer;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.integration.dtos.request.SocialRescueCandidateDto;
import iuh.fit.integration.services.SocialRescueCandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;


@Component
@RequiredArgsConstructor
public class SocialRescueCandidateConsumer {
    private final ObjectMapper objectMapper;
    private final SocialRescueCandidateService service;

    @KafkaListener(
            topics = "social-rescue-candidates",
            groupId = "integration-social-rescue-group",
            containerFactory = "socialRescueKafkaListenerContainerFactory"
    )
    public void consume(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Kafka payload không được rỗng");
        }

        // Đọc thành JSON tree để bắt buộc root là mảng.
        // Tránh vô tình chấp nhận object đơn do cấu hình coercion.
        var root = objectMapper.readTree(payload);

        if (root == null || !root.isArray()) {
            throw new BusinessException(
                     ErrorCode.BAD_GATEWAY,
                    "Kafka payload phải là mảng JSON: [{...}, {...}]"
            );
        }

        // TypeReference giữ thông tin kiểu phần tử của List khi parse.
        List<SocialRescueCandidateDto> candidates =
                objectMapper.readValue(
                        payload,
                        new TypeReference<List<SocialRescueCandidateDto>>() {}
                );

        service.ingest(candidates);
    }
}
