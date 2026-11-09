package iuh.fit.resourcemanagementservice.grpc;

import io.grpc.stub.StreamObserver;
import iuh.fit.common.grpc.*;
import iuh.fit.resourcemanagementservice.entity.CampaignTeam;
import iuh.fit.resourcemanagementservice.services.CampaignTeamService;
import iuh.fit.resourcemanagementservice.services.TeamNotificationRecipientService;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class CampaignTeamGrpcService extends ResourceTeamGrpcServiceGrpc.ResourceTeamGrpcServiceImplBase {

    private final CampaignTeamService campaignTeamService;

    private final TeamNotificationRecipientService recipientService;

    @Override
    public void getTeamByLeaderId(GetTeamByLeaderRequest request, StreamObserver<TeamInfoResponse> responseObserver) {
        UUID leaderId = UUID.fromString(request.getLeaderId());

        CampaignTeam team = campaignTeamService.getActiveTeamByLeaderId(leaderId);

        TeamInfoResponse response = TeamInfoResponse.newBuilder()
                .setCampaignTeamId(team.getId().toString())
                .setTeamName(team.getTeamName())
                .setLeaderPhone(team.getLeaderPhone())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void isLeaderOfTeam(IsLeaderOfTeamRequest request, StreamObserver<IsLeaderOfTeamResponse> responseObserver) {
        UUID leaderId = UUID.fromString(request.getLeaderId());
        UUID teamId = UUID.fromString(request.getTeamId());

        boolean isLeader = campaignTeamService.isLeaderOfTeam(leaderId, teamId);

        IsLeaderOfTeamResponse response = IsLeaderOfTeamResponse.newBuilder()
                .setIsLeader(isLeader)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    @Override
    public void getNotificationRecipientsByTeamIds(
            GetNotificationRecipientsByTeamIdsRequest request,
            StreamObserver<GetNotificationRecipientsByTeamIdsResponse> responseObserver
    ) {

        final GetNotificationRecipientsByTeamIdsResponse response;


        List<UUID> teamIds = parseTeamIds(request);

        var results = recipientService.findRecipientsByTeamIds(teamIds);

        var responseBuilder =
                GetNotificationRecipientsByTeamIdsResponse.newBuilder();

        for (var result : results) {

            var teamBuilder = TeamNotificationRecipients.newBuilder()
                    .setTeamId(result.teamId().toString())
                    .setTeamExists(result.teamExists());

            for (UUID userId : result.userIds()) {
                teamBuilder.addUserIds(userId.toString());
            }
            responseBuilder.addTeams(teamBuilder.build());
        }

        response = responseBuilder.build();

        responseObserver.onNext(response);

        responseObserver.onCompleted();
    }


    private List<UUID> parseTeamIds(
            GetNotificationRecipientsByTeamIdsRequest request
    ) {
        List<UUID> teamIds = new ArrayList<>(request.getTeamIdsCount());
        for(int i = 0; i < request.getTeamIdsCount(); i++){
            String teamIdStr = request.getTeamIds(i);
            UUID teamId = UUID.fromString(teamIdStr);
            teamIds.add(teamId);
        }
        return teamIds;
    }
}
