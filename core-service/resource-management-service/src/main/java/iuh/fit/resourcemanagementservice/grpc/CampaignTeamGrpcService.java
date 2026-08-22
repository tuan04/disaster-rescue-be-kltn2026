package iuh.fit.resourcemanagementservice.grpc;

import io.grpc.stub.StreamObserver;
import iuh.fit.common.grpc.GetTeamByLeaderRequest;
import iuh.fit.common.grpc.ResourceTeamGrpcServiceGrpc;
import iuh.fit.common.grpc.TeamInfoResponse;
import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.services.CampaignTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class CampaignTeamGrpcService extends ResourceTeamGrpcServiceGrpc.ResourceTeamGrpcServiceImplBase {

    private final CampaignTeamService campaignTeamService;

    @Override
    public void getTeamByLeaderId(GetTeamByLeaderRequest request, StreamObserver<TeamInfoResponse> responseObserver) {
        UUID leaderId = UUID.fromString(request.getLeaderId());

        CampaignTeam team = campaignTeamService.getTeamByLeaderId(leaderId);

        TeamInfoResponse response = TeamInfoResponse.newBuilder()
                .setCampaignTeamId(team.getId().toString())
                .setTeamName(team.getTeamName())
                .setLeaderPhone(team.getLeaderPhone())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
