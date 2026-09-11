package iuh.fit.dispatchservice.client;

import iuh.fit.common.grpc.GetTeamByLeaderRequest;
import iuh.fit.common.grpc.IsLeaderOfTeamRequest;
import iuh.fit.common.grpc.IsLeaderOfTeamResponse;
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

    public boolean isLeaderOfTeam(UUID leaderId, UUID teamId) {
        IsLeaderOfTeamRequest request = IsLeaderOfTeamRequest.newBuilder()
                .setLeaderId(leaderId.toString())
                .setTeamId(teamId.toString())
                .build();
        IsLeaderOfTeamResponse response = resourceTeamGrpcStub.isLeaderOfTeam(request);
        return response.getIsLeader();
    }
}
