package iuh.fit.dispatchservice.client;

import iuh.fit.common.grpc.GetTeamByLeaderRequest;
import iuh.fit.common.grpc.ResourceTeamGrpcServiceGrpc.ResourceTeamGrpcServiceBlockingStub;
import iuh.fit.common.grpc.TeamInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ResourceTeamGrpcClient {
    private final ResourceTeamGrpcServiceBlockingStub resourceTeamGrpcStub;

    public TeamInfoResponse getTeamByLeaderId(UUID leaderId) {
        GetTeamByLeaderRequest request = GetTeamByLeaderRequest.newBuilder()
                .setLeaderId(leaderId.toString())
                .build();
        return resourceTeamGrpcStub.getTeamByLeaderId(request);
    }
}
