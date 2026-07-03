package iuh.fit.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "An unexpected error occurred"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "400", "Invalid input data"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "Resource not found"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "401", "Unauthorized access"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "403", "Access denied"),
    CONFLICT(HttpStatus.CONFLICT, "409", "Resource already exists"),
    BAD_GATEWAY(HttpStatus.BAD_GATEWAY, "502", "Bad gateway");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
