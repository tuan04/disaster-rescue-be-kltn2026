package iuh.fit.dispatchservice.kafka.consumer;

import iuh.fit.common.kafka.dto.TeamLocationUpdatedEvent;
import iuh.fit.dispatchservice.services.TeamLocationTrackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TeamLocationTrackConsumer {

    private static final String LOCATION_TOPIC = "location-events";

    private final TeamLocationTrackService teamLocationTrackService;

    @KafkaListener(topics = LOCATION_TOPIC, groupId = "dispatch-location-track-group")
    public void consumeTeamLocationUpdate(TeamLocationUpdatedEvent event) {
        teamLocationTrackService.appendPointIfActive(event);
    }
}
