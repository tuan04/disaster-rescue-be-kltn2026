package iuh.fit.notificationservice.kafka.consumer;

import iuh.fit.common.kafka.dto.TeamMemberEvent;
import iuh.fit.notificationservice.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(topics = "team-events")
public class TeamEventConsumer {

    private final NotificationService notificationService;

    @KafkaHandler
    public void consumeTeamMemberEvent(TeamMemberEvent event) {
        if (event == null) {
            log.warn("Received null TeamMemberEvent from topic 'team-events' (deserialization error or tombstone message), skipping.");
            return;
        }

        if (event.eventType() == null) {
            log.warn("Received TeamMemberEvent without eventType from topic 'team-events': {}, skipping.", event);
            return;
        }

        log.info("Received TeamMemberEvent from topic 'team-events': type={}, team={}, targetUser={}",
                event.eventType(), event.teamName(), event.targetUserId());

        String title = switch (event.eventType()) {
            case APPROVED -> "Đội cứu hộ đã được phê duyệt!";
            case REJECTED -> "Yêu cầu tạo đội bị từ chối";
            case INVITED -> "Lời mời tham gia đội cứu hộ";
            case INVITATION_ACCEPTED -> "Thành viên đã gia nhập đội";
            case INVITATION_DECLINED -> "Thành viên từ chối lời mời";
            case JOIN_REQUESTED -> "Yêu cầu gia nhập đội mới";
            case JOIN_APPROVED -> "Yêu cầu gia nhập đã được duyệt!";
            case JOIN_REJECTED -> "Yêu cầu gia nhập bị từ chối";
            case MEMBER_REMOVED -> "Thông báo rời đội cứu hộ";
            case LEADER_TRANSFERRED -> "Bổ nhiệm Đội trưởng mới";
            case DISBANDED -> "Đội cứu hộ đã giải tán";
            default -> "Thông báo đội cứu hộ";
        };

        notificationService.sendNotificationToUser(
                event.targetUserId(),
                "TEAM_" + event.eventType().name(),
                title,
                event.message(),
                event.teamId()
        );
    }

    @KafkaHandler(isDefault = true)
    public void handleOtherEvents(Object object) {
        log.debug("Ignored unhandled message from topic team-events: {}", object);
    }
}
