package iuh.fit.notificationservice.kafka.consumer;

import iuh.fit.notificationservice.dtos.SOSResponse;
import iuh.fit.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendPushNotification {

    private final NotificationService notificationService;

    @KafkaListener(topics = "sos-event", groupId = "notification-group")
    public void sendPushNotification(SOSResponse event) {
        System.out.println("Received SOS event: " + event.toString());
        notificationService.processSOSEvent(event);
    }
}
