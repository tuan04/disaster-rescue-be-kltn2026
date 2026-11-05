package iuh.fit.common.kafka.dto;

public enum TeamMemberEventType {
    APPROVED,
    REJECTED,
    INVITED,
    INVITATION_ACCEPTED,
    INVITATION_DECLINED,
    JOIN_REQUESTED,
    JOIN_APPROVED,
    JOIN_REJECTED,
    MEMBER_REMOVED,
    LEADER_TRANSFERRED,
    DISBANDED
}
