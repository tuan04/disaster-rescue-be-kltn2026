package iuh.fit.userservice.service.impl;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final RestClient restClient;

    @Value("${sms.macrodroid.webhook-url:}")
    private String webhookUrl;

    public SmsService() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    public void sendOtp(String phone, String otp) {
        if (phone == null || phone.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Số điện thoại không được để trống"
            );
        }

        if (otp == null || !otp.matches("[0-9]{6}")) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "OTP phải gồm 6 chữ số"
            );
        }

        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Chưa cấu hình webhook MacroDroid"
            );
        }

        String message = "RescueSOS: Mã xác thực của bạn là "
                + otp
                + ". Mã có hiệu lực trong 5 phút. Không chia sẻ mã này.";

        URI uri = buildWebhookUri(phone.trim(), message);

        try {
            restClient.get()
                    .uri(uri)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Đã gọi webhook MacroDroid để yêu cầu gửi OTP");
        } catch (RestClientException ex) {
            // Không log URL vì chứa webhook, số điện thoại và OTP.
            log.warn("Lỗi gọi webhook MacroDroid: {}",
                    ex.getClass().getSimpleName());

            throw new BusinessException(
                    ErrorCode.BAD_GATEWAY,
                    "Chưa xác nhận được yêu cầu gửi OTP. Vui lòng thử lại sau."
            );
        }
    }

    private URI buildWebhookUri(String phone, String message) {
        try {
            String url = webhookUrl.trim();
            URI baseUri = URI.create(url);

            if (!"https".equalsIgnoreCase(baseUri.getScheme())
                    || !"trigger.macrodroid.com".equalsIgnoreCase(
                    baseUri.getHost())
                    || baseUri.getRawQuery() != null
                    || baseUri.getRawFragment() != null
                    || baseUri.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }

            return UriComponentsBuilder.fromUriString(url)
                    .queryParam("phone", "{phone}")
                    .queryParam("message", "{message}")
                    .encode()
                    .buildAndExpand(Map.of(
                            "phone", phone,
                            "message", message
                    ))
                    .toUri();
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "URL MacroDroid không hợp lệ. Dùng URL HTTPS gốc, chưa thêm Params."
            );
        }
    }
}