package iuh.fit.dispatchservice.services;

import iuh.fit.common.exception.BusinessException;
import iuh.fit.common.exception.ErrorCode;
import iuh.fit.common.grpc.TeamInfoResponse;
import iuh.fit.common.kafka.dto.RescueCompletedEvent;
import iuh.fit.dispatchservice.client.ResourceTeamGrpcClient;
import iuh.fit.dispatchservice.dtos.response.AssignmentResponse;
import iuh.fit.dispatchservice.entity.Assignment;
import iuh.fit.dispatchservice.entity.RescueRequest;
import iuh.fit.dispatchservice.enums.AssignmentStatus;
import iuh.fit.dispatchservice.enums.RequestStatus;
import iuh.fit.dispatchservice.repositories.AssignmentRepository;
import iuh.fit.dispatchservice.repositories.RescueRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import iuh.fit.common.kafka.dto.RescueCanceledEvent;
import iuh.fit.dispatchservice.kafka.producer.RescueEventProducer;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final RescueService rescueService;
    private final ResourceTeamGrpcClient resourceTeamGrpcClient;
    private final RescueEventProducer rescueEventProducer;

    private Assignment findById(UUID assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy nhiệm vụ cứu hộ"));
    }

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

    private void validateLeaderOwnsAssignment(Assignment assignment, UUID leaderId) {
        boolean isLeader;
        try {
            isLeader = resourceTeamGrpcClient.isLeaderOfTeam(leaderId, assignment.getCampaignTeamId());
        } catch (Exception e) {
            log.error("Lỗi xác thực quyền đội trưởng của leaderId {}: {}", leaderId, e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể xác thực thông tin đội cứu hộ");
        }

        if (!isLeader) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Bạn không phải là trưởng đội của đội cứu hộ được phân công nhiệm vụ này");
        }
    }

    @Transactional
    public void complete(UUID assignmentId, UUID leaderId) {
        Assignment assignment = findById(assignmentId);

        if (assignment.getStatus() != AssignmentStatus.ACCEPTED) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Chỉ có thể hoàn thành nhiệm vụ đang ở trạng thái ACCEPTED");
        }

        validateLeaderOwnsAssignment(assignment, leaderId);

        assignment.setStatus(AssignmentStatus.COMPLETED);
        LocalDateTime now = LocalDateTime.now();
        assignment.setCompletedAt(now);

        RescueRequest rescueRequest = assignment.getRescueRequest();
        rescueRequest.setStatus(RequestStatus.COMPLETED);
        rescueRequest.setCompleteAt(now);

        RescueCompletedEvent event = new RescueCompletedEvent(
                assignment.getId(),
                rescueRequest.getId(),
                assignment.getCampaignTeamId(),
                java.time.Instant.now());
        rescueEventProducer.publishRescueCompletedEvent(event);
    }

    @Transactional
    public void cancel(UUID assignmentId, UUID leaderId, String reason) {
        Assignment assignment = findById(assignmentId);

        if (assignment.getStatus() != AssignmentStatus.ACCEPTED) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT,
                    "Chỉ có thể hủy nhiệm vụ đang ở trạng thái ACCEPTED");
        }

        validateLeaderOwnsAssignment(assignment, leaderId);

        assignment.setStatus(AssignmentStatus.CANCELED);
        assignment.setNotes(reason.trim());

        RescueRequest rescueRequest = assignment.getRescueRequest();
        rescueRequest.setStatus(RequestStatus.PENDING);

        RescueCanceledEvent event = new RescueCanceledEvent(
                assignment.getId(),
                rescueRequest.getId(),
                assignment.getCampaignTeamId(),
                reason.trim(),
                java.time.Instant.now());
        rescueEventProducer.publishRescueCanceledEvent(event);
    }

    public AssignmentResponse getActiveMissionByTeam(UUID teamId) {
        if (teamId == null) {
            return null;
        }

        return assignmentRepository
                .findByCampaignTeamIdAndStatus(teamId, AssignmentStatus.ACCEPTED)
                .map(AssignmentResponse::fromEntity)
                .orElse(null);
    }
}
