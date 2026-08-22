package iuh.fit.dispatchservice.services;

import iuh.fit.common.grpc.TeamInfoResponse;
import iuh.fit.dispatchservice.client.ResourceTeamGrpcClient;
import iuh.fit.dispatchservice.entity.Assignment;
import iuh.fit.dispatchservice.entity.RescueRequest;
import iuh.fit.dispatchservice.enums.AssignmentStatus;
import iuh.fit.dispatchservice.enums.RequestStatus;
import iuh.fit.dispatchservice.repositories.AssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final RescueService rescueService;
    private final ResourceTeamGrpcClient resourceTeamGrpcClient;

    private Assignment createAssignment(UUID requestId, UUID leaderId, String notes, AssignmentStatus initialStatus) {
        RescueRequest rescueRequest = rescueService.getRescueRequest(requestId);

        TeamInfoResponse teamInfo = resourceTeamGrpcClient.getTeamByLeaderId(leaderId);

        Assignment assignment = Assignment.builder()
                .rescueRequest(rescueRequest)
                .campaignTeamId(UUID.fromString(teamInfo.getCampaignTeamId()))
                .assignedTeamName(teamInfo.getTeamName())
                .leaderPhone(teamInfo.getLeaderPhone())
                .status(initialStatus)
                .notes(notes)
                .build();

        if (initialStatus == AssignmentStatus.ACCEPTED) {
            rescueRequest.setStatus(RequestStatus.ACCEPTED);
        }

        return assignmentRepository.save(assignment);
    }

    @Transactional
    public Assignment acceptRescueByLeader(UUID requestId, UUID leaderId, String notes) {
        return createAssignment(requestId, leaderId, notes, AssignmentStatus.ACCEPTED);
    }

}
