package iuh.fit.common.grpc;

import io.grpc.Status;
import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GrpcStatusMapper {

    private GrpcStatusMapper() {
    }

    public static Status toGrpcStatus(Exception e) {
        if (e instanceof BusinessException be) {
            return mapBusinessException(be);
        }

        if (e instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT.withDescription(e.getMessage());
        }

        if (e instanceof IllegalStateException) {
            return Status.FAILED_PRECONDITION.withDescription(e.getMessage());
        }

        if (e instanceof SecurityException) {
            return Status.PERMISSION_DENIED.withDescription(e.getMessage());
        }

        log.error("Unhandled gRPC exception", e);
        return Status.INTERNAL.withDescription("Internal server error");
    }

    private static Status mapBusinessException(BusinessException be) {
        ErrorCode errorCode = be.getErrorCode();
        String message = be.getMessage();

        if (errorCode == null) {
            log.error("BusinessException with null ErrorCode", be);
            return Status.INTERNAL.withDescription("Internal server error");
        }

        return switch (errorCode) {
            case RESOURCE_NOT_FOUND -> Status.NOT_FOUND.withDescription(message);
            case INVALID_INPUT -> Status.INVALID_ARGUMENT.withDescription(message);
            case UNAUTHORIZED -> Status.UNAUTHENTICATED.withDescription(message);
            case FORBIDDEN -> Status.PERMISSION_DENIED.withDescription(message);
            case CONFLICT -> Status.FAILED_PRECONDITION.withDescription(message);
            case BAD_GATEWAY -> Status.UNAVAILABLE.withDescription(message);
            case INTERNAL_SERVER_ERROR -> {
                log.error("BusinessException with INTERNAL_SERVER_ERROR: {}", message);
                yield Status.INTERNAL.withDescription("Internal server error");
            }
        };
    }
}
