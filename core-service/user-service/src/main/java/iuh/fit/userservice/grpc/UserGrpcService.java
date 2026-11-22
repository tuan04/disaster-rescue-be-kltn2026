package iuh.fit.userservice.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import iuh.fit.common.grpc.*;
import iuh.fit.userservice.entity.User;
import iuh.fit.userservice.repository.UserRepository;
import iuh.fit.userservice.service.impl.UserServiceImpl;
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
    private final UserServiceImpl userService;

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
                        .setRole(user.getEffectiveRole() != null ? user.getEffectiveRole() : "")
                        .setAvatarUrl(user.getAvatarUrl() != null ? user.getAvatarUrl() : "")
                        .setSex(user.getSex() != null ? user.getSex().name() : "")
                        .build())
                .toList();

        GetUsersByIdsResponse response = GetUsersByIdsResponse.newBuilder()
                .addAllUsers(userResponses)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    @Override
    public void getNotificationUserIds(
            GetNotificationUserIdsRequest request,
            StreamObserver<GetNotificationUserIdsResponse> observer
    ) {
        int page = request.getPage();
        int size = request.getSize();

        if (page < 0 || size < 1 || size > 1000) {
            observer.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(
                                    "page phải >= 0 và size phải từ 1 đến 1000"
                            )
                            .asRuntimeException()
            );
            return;
        }

        try {
            var userPage = userService.getUserIds(page, size);

            var response = GetNotificationUserIdsResponse.newBuilder()
                    .addAllUserIds(
                            userPage.getContent().stream()
                                    .map(UUID::toString)
                                    .toList()
                    )
                    .setHasNext(userPage.hasNext())
                    .build();

            observer.onNext(response);
            observer.onCompleted();

        } catch (Exception exception) {
            log.error("Không lấy được danh sách userId nhận thông báo", exception);

            observer.onError(
                    Status.INTERNAL
                            .withDescription(
                                    "Không lấy được danh sách người nhận"
                            )
                            .asRuntimeException()
            );
        }
    }
}
