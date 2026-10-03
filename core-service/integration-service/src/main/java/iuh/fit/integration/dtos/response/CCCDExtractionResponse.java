package iuh.fit.integration.dtos.response;

public record CCCDExtractionResponse(
        Boolean identityCard,
        String CCCDNumber,
        String fullName,
        String birthDay,
        String gender,
        String address
) {
}
