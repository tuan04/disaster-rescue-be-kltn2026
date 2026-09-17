package iuh.fit.dispatchservice.client;

import iuh.fit.common.grpc.GetUsersByIdsRequest;
import iuh.fit.common.grpc.GetUsersByIdsResponse;
import iuh.fit.common.grpc.UserInfoGrpcResponse;
import iuh.fit.common.grpc.UserGrpcServiceGrpc.UserGrpcServiceBlockingStub;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class UserGrpcClient {
    private final UserGrpcServiceBlockingStub userGrpcStub;

    public Map<UUID, UserInfoGrpcResponse> getUsersByIds(Set<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }


        List<String> idStrings = userIds.stream()
                .filter(Objects::nonNull)
                .map(UUID::toString)
                .toList();

        if (idStrings.isEmpty()) {
            return Collections.emptyMap();
        }

        GetUsersByIdsRequest request = GetUsersByIdsRequest.newBuilder()
                .addAllUserIds(idStrings)
                .build();

        GetUsersByIdsResponse response = userGrpcStub.getUsersByIds(request);

        return response.getUsersList().stream()
                .filter(user -> user.getId() != null && !user.getId().isBlank())
                .collect(Collectors.toMap(
                        user -> UUID.fromString(user.getId()),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
    }
}
