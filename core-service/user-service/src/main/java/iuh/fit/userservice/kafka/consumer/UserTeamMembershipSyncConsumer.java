package iuh.fit.userservice.kafka.consumer;

import iuh.fit.common.kafka.dto.UserTeamMembershipSyncEvent;
import iuh.fit.userservice.enums.RoleInTeamEnum;
import iuh.fit.userservice.repository.VolunteerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserTeamMembershipSyncConsumer {

    private final VolunteerProfileRepository volunteerProfileRepository;

    @Transactional
    @KafkaListener(topics = UserTeamMembershipSyncEvent.TOPIC_NAME, groupId = "user-service-group")
    public void consumeUserTeamSyncEvent(UserTeamMembershipSyncEvent event) {
        if (event == null || event.userId() == null) {
            return;
        }

        RoleInTeamEnum targetRole = RoleInTeamEnum.RESCUER;
        if (event.roleInTeam() != null && !event.roleInTeam().isBlank()) {
            try {
                targetRole = RoleInTeamEnum.valueOf(event.roleInTeam().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid roleInTeam '{}' for user {}; defaulting to RESCUER",
                        event.roleInTeam(), event.userId());
            }
        }

        final RoleInTeamEnum finalRole = targetRole;
        volunteerProfileRepository.findById(event.userId()).ifPresentOrElse(
                profile -> {
                    profile.setTeamId(event.teamId());
                    profile.setTeamName(event.teamName());
                    profile.setCurrentRoleInTeam(finalRole);
                    volunteerProfileRepository.save(profile);
                    log.info("Synced VolunteerProfile for user {}: teamId={}, roleInTeam={}",
                            event.userId(), event.teamId(), finalRole);
                },
                () -> log.warn("VolunteerProfile not found for userId={} during team sync", event.userId())
        );
    }
}
