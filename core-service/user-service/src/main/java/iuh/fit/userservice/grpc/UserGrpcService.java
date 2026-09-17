package iuh.fit.userservice.grpc;

import io.grpc.stub.StreamObserver;
import iuh.fit.common.grpc.GetUsersByIdsRequest;
import iuh.fit.common.grpc.GetUsersByIdsResponse;
import iuh.fit.common.grpc.UserInfoGrpcResponse;
import iuh.fit.common.grpc.UserGrpcServiceGrpc;
import iuh.fit.userservice.entity.User;
import iuh.fit.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcService extends UserGrpcServiceGrpc.UserGrpcServiceImplBase {

    private final UserRepository userRepository;

    @Override
    public void getUsersByIds(GetUsersByIdsRequest request, StreamObserver<GetUsersByIdsResponse> responseObserver) {
  
        List<UUID> userIds = request.getUserIdsList().stream()
                .filter(Objects::nonNull)
                .map(UUID::fromString)
                .toList();

        List<User> users = userRepository.findAllById(userIds);

        List<UserInfoGrpcResponse> userResponses = users.stream()
                .map(user -> UserInfoGrpcResponse.newBuilder()
                        .setId(user.getId().toString())
                        .setFullName(user.getFullName() != null ? user.getFullName() : "")
                        .setPhoneNumber(user.getPhone() != null ? user.getPhone() : "")
                        .build())
                .toList();

        GetUsersByIdsResponse response = GetUsersByIdsResponse.newBuilder()
                .addAllUsers(userResponses)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
