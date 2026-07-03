package iuh.fit.common.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorResponse {
    private boolean success;
    private String code;
    private String message;
    private Map<String, String> details; // For validation field errors
    private LocalDateTime timestamp;

    public static ErrorResponse of(String code, String message) {
        return ErrorResponse.builder()
                .success(false)
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse of(String code, String message, Map<String, String> details) {
        return ErrorResponse.builder()
                .success(false)
                .code(code)
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
