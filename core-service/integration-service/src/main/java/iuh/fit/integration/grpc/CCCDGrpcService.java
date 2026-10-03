package iuh.fit.integration.grpc;

import io.grpc.stub.StreamObserver;
import iuh.fit.common.grpc.CCCDImageGrpcResponse;
import iuh.fit.common.grpc.GrpcStatusMapper;
import iuh.fit.common.grpc.integration_serviceGrpc;
import iuh.fit.common.grpc.CCCDImageGrpcRequest;
import iuh.fit.integration.dtos.response.CCCDExtractionResponse;
import iuh.fit.integration.services.CCCDExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;


@GrpcService
@RequiredArgsConstructor
public class CCCDGrpcService extends integration_serviceGrpc.integration_serviceImplBase{
    private final CCCDExtractionService cccdExtractionService;

    @Override
    public void getCCCDInfoFromImage(
            CCCDImageGrpcRequest request,
            StreamObserver<CCCDImageGrpcResponse> responseObserver
    ){
        CCCDImageGrpcResponse response;

        try {
            CCCDExtractionResponse result = cccdExtractionService.extract(
                    request.getImageData().toByteArray()
            );

            response = CCCDImageGrpcResponse.newBuilder()
                    .setCCCDNumber(result.CCCDNumber())
                    .setFullName(result.fullName())
                    .setBirthDay(result.birthDay())
                    .setGender(result.gender())
                    .setAddress(result.address())
                    .build();

        } catch (Exception exception) {
            responseObserver.onError(
                    GrpcStatusMapper.toGrpcStatus(exception)
                            .asRuntimeException()
            );
            return;
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }




}
