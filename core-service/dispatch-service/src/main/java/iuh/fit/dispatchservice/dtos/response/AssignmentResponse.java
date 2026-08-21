package iuh.fit.dispatchservice.dtos.response;

import iuh.fit.dispatchservice.entity.Assignment;
import iuh.fit.dispatchservice.enums.AssignmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AssignmentResponse {

    private UUID id;
    private UUID requestId;
    private UUID campaignTeamId;
    private String assignedTeamName;
    private String leaderPhone;
    private AssignmentStatus status;
    private LocalDateTime assignedAt;
    private String notes;

    public static AssignmentResponse fromEntity(Assignment assignment) {
        return AssignmentResponse.builder()
                .id(assignment.getId())
                .requestId(assignment.getRescueRequest().getId())
                .campaignTeamId(assignment.getCampaignTeamId())
                .assignedTeamName(assignment.getAssignedTeamName())
                .leaderPhone(assignment.getLeaderPhone())
                .status(assignment.getStatus())
                .assignedAt(assignment.getAssignedAt())
                .notes(assignment.getNotes())
                .build();
    }
}
