package iuh.fit.integration.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.kafka.dto.SOSRequest;
import iuh.fit.integration.dtos.response.SMSRequest;
import iuh.fit.integration.kafka.producer.SmsEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class SmsService {
    private final SmsEventProducer smsEventProducer;

    public void processAndSendSmsSOS(SMSRequest smsRequest) {
        if (smsRequest == null || smsRequest.rawMessage() == null || smsRequest.rawMessage().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Nội dung tin nhắn SMS không được để trống");
        }
        if(smsRequest.senderPhone() == null || smsRequest.senderPhone().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Số điện thoại người gửi SMS không được để trống");
        }

        String rawMessage = smsRequest.rawMessage().trim();
        String[] parts = rawMessage.split(",");

        if (parts.length < 3) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "Định dạng tin nhắn SMS không hợp lệ. Yêu cầu: [số điện thoại], [nội dung], [vĩ độ], [kinh độ]");
        }

        Double latitude;
        Double longitude;
        try {
            latitude = Double.parseDouble(parts[parts.length - 2].trim());
            longitude = Double.parseDouble(parts[parts.length - 1].trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Tọa độ vĩ độ hoặc kinh độ không hợp lệ");
        }

        if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Tọa độ nằm ngoài phạm vi địa lý hợp lệ");
        }

        String reporterPhone = normalizePhone(smsRequest.senderPhone());
        String content = parts[parts.length - 3].trim();

        SOSRequest smsEvent = SOSRequest.builder()
                .reporterPhone(reporterPhone)
                .content(content)
                .latitude(latitude)
                .longitude(longitude)
                .build();
        smsEventProducer.publishSmsEvent(smsEvent);

    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String cleaned = phone.trim().replaceAll("[^0-9+]", "");
        if (cleaned.startsWith("+84")) {
            cleaned = "0" + cleaned.substring(3);
        } else if (cleaned.startsWith("84") && cleaned.length() == 11) {
            cleaned = "0" + cleaned.substring(2);
        }
        return cleaned;
    }
}
