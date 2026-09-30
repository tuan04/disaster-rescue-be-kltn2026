package iuh.fit.integration.dtos.response;

public record SMSRequest(
        String senderPhone,
        String rawMessage
) {
}
