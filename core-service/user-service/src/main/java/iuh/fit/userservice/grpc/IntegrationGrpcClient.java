package iuh.fit.userservice.grpc;


import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.grpc.CCCDImageGrpcRequest;
import iuh.fit.common.grpc.CCCDImageGrpcResponse;
import iuh.fit.common.grpc.integration_serviceGrpc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class IntegrationGrpcClient {
    private final integration_serviceGrpc.integration_serviceBlockingStub stub;

    public CCCDImageGrpcResponse extractCccd(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Vui lòng gửi ảnh CCCD"
            );
        }

        if (image.getSize() > 5L * 1024 * 1024) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Ảnh CCCCD không được vượt quá 5 MiB"
            );
        }

        try {
            String fileName = image.getOriginalFilename();

            CCCDImageGrpcRequest request =
                    CCCDImageGrpcRequest.newBuilder()
                            .setImageData(ByteString.copyFrom(image.getBytes()))
                            .setFileName(fileName == null ? "" : fileName)
                            .build();

            return stub.withDeadlineAfter(60, TimeUnit.SECONDS)
                    .getCCCDInfoFromImage(request);
        } catch (IOException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Không đọc được file ảnh"
            );
        } catch (StatusRuntimeException exception) {
            if (exception.getStatus().getCode()
                    == Status.Code.INVALID_ARGUMENT) {
                throw new BusinessException(
                        ErrorCode.INVALID_INPUT,
                        "Ảnh không hợp lệ hoặc không đủ rõ để trích xuất"
                );
            }

            throw new BusinessException(
                    ErrorCode.BAD_GATEWAY,
                    "Dịch vụ đọc CCCD chưa phản hồi, vui lòng thử lại"
            );
        }
    }

}
