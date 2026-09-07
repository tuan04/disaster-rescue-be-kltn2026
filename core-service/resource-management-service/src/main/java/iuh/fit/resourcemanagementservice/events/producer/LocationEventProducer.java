package iuh.fit.resourcemanagementservice.events.producer;

import iuh.fit.common.kafka.dto.TeamLocationUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class LocationEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    final String LOCATION_TOPIC = "location-events";

    public void publishTeamLocationUpdateEvent(TeamLocationUpdatedEvent teamLocation) {
        kafkaTemplate.send(
                LOCATION_TOPIC,
                teamLocation.teamId().toString(),
                teamLocation
        );
    }
}
