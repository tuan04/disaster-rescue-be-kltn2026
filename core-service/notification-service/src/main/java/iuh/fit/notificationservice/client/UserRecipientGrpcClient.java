package iuh.fit.notificationservice.client;

import iuh.fit.common.grpc.GetNotificationUserIdsRequest;
import iuh.fit.common.grpc.UserGrpcServiceGrpc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class UserRecipientGrpcClient {

    private static final int PAGE_SIZE = 500;
    private static final long TIMEOUT_SECONDS = 5;

    private final UserGrpcServiceGrpc.UserGrpcServiceBlockingStub userStub;

    public List<UUID> findAllUserIds() {
        List<UUID> userIds = new ArrayList<>();
        int page = 0;

        while (true) {
            var request = GetNotificationUserIdsRequest.newBuilder()
                    .setPage(page)
                    .setSize(PAGE_SIZE)
                    .build();

            var response = userStub
                    .withDeadlineAfter(
                            TIMEOUT_SECONDS,
                            TimeUnit.SECONDS
                    )
                    .getNotificationUserIds(request);

            for (String rawUserId : response.getUserIdsList()) {
                userIds.add(UUID.fromString(rawUserId));
            }

            if (!response.getHasNext()) {
                break;
            }

            page++;
        }

        return List.copyOf(userIds);
    }
}