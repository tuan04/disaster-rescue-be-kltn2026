package iuh.fit.notificationservice.kafka.consumer;

import iuh.fit.common.kafka.dto.TeamLocationUpdatedEvent;
import iuh.fit.notificationservice.socket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LocationEventConsumer {
    private final WebSocketNotificationService webSocketNotificationService;
    private static final String LOCATION_TOPIC = "location-events";

    @KafkaListener(topics = LOCATION_TOPIC)
    public void consumTeamLocationUpdate(TeamLocationUpdatedEvent teamLocation) {
        log.info("Received location update: {}", teamLocation);

        webSocketNotificationService.broadcast("/topic/teams/%s/location".formatted(teamLocation.teamId()),
                teamLocation);
    }
}
